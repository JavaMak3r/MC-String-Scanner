# Minecraft Memory Scanner

## English Description

### 🔍 About the Project
**Minecraft Memory Scanner** is a tool for detecting cheats by scanning Minecraft's memory for specific strings.

### 🛠 How It Works
This program reads a list of pre-defined strings (from `strings.txt`) and searches Minecraft memory for them. If a match is found, it is output to the console. This method is useful for detecting cheats from known strings.

### ✨ Features
- Scans Minecraft’s memory for specific cheat-related strings
- Automatically detects Minecraft’s process ID
- Uses multi-threading for efficient searching
- Displays results with color-coded output

### 📌 Requirements
- Windows OS
- Java 16 or higher
- Administrator privileges (to access process memory)

### 🚀 How to Use
1. **Download** the project.
2. **Place a list of strings** to search for in `resources/strings.txt`.
3. **Build the project** using:
   ```sh
   ./gradlew clean shadowJar
   ```
4. **Run the program** using:
   ```sh
   java -Dfile.encoding=UTF-8 -jar strings-1.0-SNAPSHOT.jar
   ```
5. **Check the console** for detected strings.

### ⚠ Important Compatibility Information
- **Windows Only** - This program uses WinAPI via JNA and works exclusively on Windows.
- **Tested on Windows 11** - Development and testing were conducted on Windows 11.

### ⚠ Disclaimer
This tool is provided "as is" without any warranties. The author is not responsible for any use of this code.

---

## Русское описание

### 🔍 О проекте
**Minecraft Memory Scanner** — это инструмент для обнаружения читов путем сканирования памяти Minecraft на наличие определенных строк.

### 🛠 Как это работает
Эта программа считывает список заранее заданных строк (из `strings.txt`) и ищет их в памяти Minecraft. Если совпадение найдено, оно выводится в консоль. Этот метод полезен для обнаружения читов по известным строкам.

### ✨ Возможности
- Сканирование памяти Minecraft на наличие строк, связанных с читами
- Автоматическое определение идентификатора процесса Minecraft
- Использование многопоточности для ускорения поиска
- Вывод результатов в цветном формате

### 📌 Требования
- Windows OS
- Java 16 или выше
- Администраторские права (для доступа к памяти процесса)

### 🚀 Как использовать
1. **Скачайте** проект.
2. **Добавьте список строк** для поиска в `resources/strings.txt`.
3. **Соберите проект** с помощью команды:
   ```sh
   ./gradlew clean shadowJar
   ```
4. **Запустите программу** с помощью команды:
   ```sh
   java -Dfile.encoding=UTF-8 -jar strings-1.0-SNAPSHOT.jar
   ```
5. **Проверьте консоль**, чтобы увидеть найденные строки.

### ⚠ Важная информация о совместимости
- **Только для Windows** - программа использует WinAPI через JNA и работает исключительно в среде Windows.
- **Протестировано на Windows 11** - разработка и тестирование проводились на Windows 11.

### ⚠ Предупреждение
Этот инструмент предоставляется "как есть", без каких-либо гарантий. Автор не несет ответственности за любое использование этого кода.

