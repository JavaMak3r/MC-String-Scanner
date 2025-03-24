package org.example;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

public class MinecraftMemoryScanner {

    private static final int PROCESS_VM_READ = 0x0010;
    private static final int PROCESS_QUERY_INFORMATION = 0x0400;
    private static final WinDef.DWORD TH32CS_SNAPPROCESS = new WinDef.DWORD(0x00000002L);

    // Console colors
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) {
        try {
            printBanner();

            List<String> searchStrings = readStringsFromResources();
            if (searchStrings.isEmpty()) {
                System.out.println(YELLOW + "[!] No search strings found in resources" + RESET);
                return;
            }

            List<Integer> pids = findMinecraftPids();
            if (pids.isEmpty()) {
                System.out.println(YELLOW + "[!] Minecraft not found. Please ensure:");
                System.out.println("1. Minecraft is running");
                System.out.println("2. Using standard launcher" + RESET);
                return;
            }

            for (int pid : pids) {
                System.out.println(CYAN + "\n[~] Scanning Minecraft (PID: " + pid + ")..." + RESET);

                searchStrings.parallelStream().forEach(searchString -> {
                    System.out.println(CYAN + "[~] Searching for exact match: \"" + GREEN + searchString + CYAN + "\"" + RESET);
                    scanMemory(pid, searchString);
                });
            }

            System.out.println(CYAN + "\n[✓] Scanning completed" + RESET);
        } catch (Exception e) {
            System.out.println("[X] Error: " + e.getMessage());
        }
    }

    private static List<String> readStringsFromResources() {
        try (InputStream is = MinecraftMemoryScanner.class.getResourceAsStream("/strings.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println(YELLOW + "[!] Error reading strings.txt: " + e.getMessage() + RESET);
            return Collections.emptyList();
        }
    }

    private static void printBanner() {
        System.out.println(CYAN + "┌──────────────────────┐");
        System.out.println("│  MC String Scanner  │");
        System.out.println("└──────────────────────┘" + RESET);
    }

    private static List<Integer> findMinecraftPids() {
        List<Integer> pids = new ArrayList<>();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                TH32CS_SNAPPROCESS,
                new WinDef.DWORD(0));

        if (snapshot == null) {
            System.out.println(YELLOW + "[!] Failed to get process list" + RESET);
            return pids;
        }

        try {
            Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
            processEntry.dwSize = new WinDef.DWORD(processEntry.size());

            if (Kernel32.INSTANCE.Process32First(snapshot, processEntry)) {
                do {
                    String exeFile = processEntry.szExeFile.toString();
                    if (exeFile.equalsIgnoreCase("javaw.exe") ||
                            exeFile.toLowerCase().contains("minecraft")) {
                        pids.add(processEntry.th32ProcessID.intValue());
                    }
                } while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry));
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        if (pids.isEmpty()) {
            pids = findPidsViaPowerShell();
        }

        return pids;
    }

    private static List<Integer> findPidsViaPowerShell() {
        List<Integer> pids = new ArrayList<>();
        try {
            Process process = new ProcessBuilder()
                    .command("powershell", "-Command", "Get-Process javaw,minecraft* | Select-Object -ExpandProperty Id")
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        pids.add(Integer.parseInt(line.trim()));
                    } catch (NumberFormatException e) {
                        // Ignore non-numeric lines
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.out.println(YELLOW + "[!] PowerShell error: " + e.getMessage() + RESET);
        }
        return pids;
    }

    private static void scanMemory(int pid, String searchString) {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                PROCESS_VM_READ | PROCESS_QUERY_INFORMATION,
                false,
                pid);

        if (processHandle == null) {
            System.out.println(YELLOW + "[!] Failed to open process (no permissions?)" + RESET);
            return;
        }

        try {
            byte[] searchBytes = searchString.getBytes(StandardCharsets.UTF_8);
            int bufferSize = 1024 * 1024; // 1MB buffer
            Memory buffer = new Memory(bufferSize);

            Kernel32.MEMORY_BASIC_INFORMATION memoryInfo = new Kernel32.MEMORY_BASIC_INFORMATION();
            Pointer pointer = Pointer.NULL;
            BaseTSD.SIZE_T size = new BaseTSD.SIZE_T(memoryInfo.size());
            BaseTSD.SIZE_T zero = new BaseTSD.SIZE_T(0);

            while (true) {
                BaseTSD.SIZE_T result = Kernel32.INSTANCE.VirtualQueryEx(processHandle, pointer, memoryInfo, size);
                if (result.equals(zero)) {
                    break;
                }

                if (memoryInfo.state.intValue() == 0x1000 &&
                        (memoryInfo.protect.intValue() == 0x04 || memoryInfo.protect.intValue() == 0x02)) {

                    scanMemoryRegion(processHandle, pointer, memoryInfo.regionSize.intValue(),
                            searchBytes, buffer);
                }

                long newAddress = Pointer.nativeValue(pointer) + memoryInfo.regionSize.longValue();
                if (newAddress < Pointer.nativeValue(pointer)) break;
                pointer = new Pointer(newAddress);
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
    }

    private static void scanMemoryRegion(WinNT.HANDLE processHandle, Pointer pointer,
                                         int regionSize, byte[] searchBytes, Memory buffer) {
        long address = Pointer.nativeValue(pointer);
        long endAddress = address + regionSize;
        IntByReference bytesRead = new IntByReference();

        while (address < endAddress) {
            int chunkSize = (int) Math.min(buffer.size(), endAddress - address);

            if (Kernel32.INSTANCE.ReadProcessMemory(processHandle, new Pointer(address),
                    buffer, chunkSize, bytesRead)) {
                byte[] bytes = buffer.getByteArray(0, bytesRead.getValue());
                findExactMatch(bytes, bytesRead.getValue(), searchBytes);
            }
            address += chunkSize;
        }
    }

    private static void findExactMatch(byte[] buffer, int length, byte[] searchBytes) {
        for (int i = 0; i <= length - searchBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < searchBytes.length; j++) {
                if (buffer[i + j] != searchBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                boolean validBoundaryBefore = (i == 0) || (buffer[i-1] <= 0x20);
                boolean validBoundaryAfter = (i + searchBytes.length >= length) ||
                        (buffer[i + searchBytes.length] <= 0x20);

                if (validBoundaryBefore && validBoundaryAfter) {
                    System.out.println(GREEN + new String(searchBytes) + RESET);
                    break;
                }
            }
        }
    }
}