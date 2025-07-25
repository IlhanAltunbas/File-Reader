📁 FileManager – Android Application
FileManager is a modern Android application that allows users to manage files on their device. Users can view, add, search, and delete files easily through an intuitive interface.

Built with Jetpack Compose and following the MVVM architecture, FileManager offers a clean and scalable codebase using modern Android development practices.

🚀 Key Features
📂 File Listing: Displays all stored files in a structured list.

➕ Add Files: Allows users to select and add files from their device.

Metadata (name, size, MIME type, last modified date, URI) is saved in the database.

For supported file types (PDF, DOC, XLS), the author metadata is extracted.

🔍 Search Files:

Search by file name.

Search modes like contains and starts with, managed by the SearchType enum.

🗑️ Delete Files: Removes selected files from the database and UI list.

🔐 Persistent URI Permissions: Manages permanent access to selected files.

📑 MIME Type Detection: Automatically determines MIME type via file extension or ContentResolver.

🧰 Technologies Used
Technology	Description
Kotlin	Programming language
Jetpack Compose	Modern UI toolkit (Material 3, Navigation, Activity Compose)
MVVM	Architectural pattern
Hilt	Dependency injection
Room	Local database
Coroutines	Asynchronous programming (viewModelScope, Dispatchers.IO)
Lifecycle	ViewModel & UI state management
Apache POI	Reads metadata from .doc and .xls files
PDFBox Android	Extracts author info from PDF files

🧠 Architecture – MainViewModel Focus
The core logic is handled by MainViewModel, which maintains UI state and coordinates with the repository layer.

📌 States
filesList: State<List<Files>> – File list shown in the UI

searchKey: State<String> – Input text for search

selectedSearchType: State<SearchType> – Current search type

isSearching: State<Boolean> – Indicates whether search mode is active

🔧 Functions
fetchAllFilesToList() – Loads all files from the repository

onSearchKeyChanged() – Triggered when the search input changes

onSearchTypeChanged() – Triggered when search type is updated

toggleSearch() – Enables/disables search mode

performSearch() – Filters files based on current search key and type

delete(fileId) – Deletes a file by its ID

processSelectedFile(uri) – Handles file selection and metadata extraction

getFileExtension(filename) – Extracts file extension from name

🧪 Setup & Run
Clone the repository:

bash
Kopyala
Düzenle
git clone https://github.com/your-username/filemanager.git
cd filemanager
Open the project in Android Studio

Make sure required SDKs and tools are installed

Wait for Gradle Sync to complete

Run the app on an emulator or physical Android device
