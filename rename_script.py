import os
import glob

search_dir = r"d:\IT Data\Book Tracker\app\src\main\java\com\example\booktracker\app"
for root, dirs, files in os.walk(search_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content.replace("totalUnits", "totalPages").replace("currentUnit", "currentPage").replace("startUnit", "startPage").replace("endUnit", "endPage").replace("unitsRead", "pagesRead")
            
            if new_content != content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"Updated {path}")
