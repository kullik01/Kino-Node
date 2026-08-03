import os
import glob

replacements = {
    "/images/logo": "/images/logos/logo",
    "/images/app_logo_cursor": "/images/logos/app_logo_cursor",
    "/images/error_icon": "/images/icons/error_icon",
    "/images/info_icon": "/images/icons/info_icon",
    "/images/removal_icon": "/images/icons/removal_icon",
    "/images/question_mark": "/images/icons/question_mark",
    "/images/placeholder_cover": "/images/placeholders/placeholder_cover"
}

files_to_check = []
files_to_check.extend(glob.glob("src/main/java/**/*.java", recursive=True))
files_to_check.append("build.gradle.kts")

for filepath in files_to_check:
    if not os.path.isfile(filepath):
        continue
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = content
    for old_str, new_str in replacements.items():
        new_content = new_content.replace(old_str, new_str)
        
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
