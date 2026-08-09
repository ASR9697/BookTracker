import os

def fix_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content
    for old, new in replacements:
        new_content = new_content.replace(old, new)
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {path}")

app_dir = r"d:\IT Data\Book Tracker\app\src\main\java\com\example\booktracker\app"
shared_dir = r"d:\IT Data\Book Tracker\shared\src\main\java\com\example\booktracker\shared"

fix_file(os.path.join(shared_dir, "models", "Models.kt"), [
    ("val environmentTag: String = \"\"", "val environmentTag: String = \"\",\n    val deviceSource: String = \"phone\""),
    ("val text: String,", "val text: String,\n    val markdownContent: String = \"\","),
    ("val rating: Float = 0f", "val rating: Map<String, Float> = emptyMap(),\n    val dnfData: DnfData? = null"),
    ("val rating: Float", "val rating: Map<String, Float>"),
    ("enum class BookStatus { TO_READ, SHORTLIST, UP_NEXT, READING, PAUSED, FINISHED, DNF }", "enum class BookStatus { TO_READ, SHORTLIST, UP_NEXT, READING, PAUSED, FINISHED, DNF }\n\nobject RatingAxis {\n    val ALL = listOf(\"Writing\", \"Characters\", \"Plot\", \"Pacing\", \"Enjoyment\")\n}\n\nobject DnfReasons {\n    val ALL = listOf(\"Boring\", \"Bad Writing\", \"Too long\", \"Not for me\")\n}\n\ndata class DnfData(val abandonedPercentage: Float, val reason: String)")
])

for root, dirs, files in os.walk(app_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            fix_file(path, [
                ("BookStatus.BACKLOG", "BookStatus.SHORTLIST"),
                ("pageOrUnit", "page"),
                ("formatRating", "String.format(\"%.1f\", it) //"),
                ("Icons.Filled.Check", "androidx.compose.material.icons.Icons.Filled.Check")
            ])

fix_file(os.path.join(app_dir, "data", "BookRepository.kt"), [
    ("authors = authors,", "creators = authors.map { Creator(it, \"Author\") },"),
    ("genres = genres,", "classification = Classification(genres, emptyList()),"),
    ("publishedDate = publishedDate", "publication = Publication(\"\", publishedDate), format = BookFormat.Paperback"),
    ("val book = Book(", "val book = Book(\n            dateAdded = System.currentTimeMillis(),")
])

fix_file(os.path.join(app_dir, "ui", "LibrarySearchScreen.kt"), [
    ("format = book.format.lowercase()", "format = book.format.name.lowercase()")
])
