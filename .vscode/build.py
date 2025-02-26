import json5
import os
import glob

settings_path = "./.vscode/settings.json"
javac_path = r"C:\\Program Files (x86)\\Java\\JDK7\\bin\\javac"
jar_path = r"C:\\Program Files (x86)\\Java\\JDK7\\bin\\jar"
winrar_path = r"C:\\Program Files\\WinRAR\\Rar.exe"

with open(settings_path, "r", encoding="utf-8") as f:
    print(f"Chargement du fichier {settings_path}")
    settings = json5.load(f)

libraries = settings.get("java.project.referencedLibraries", [])

expanded_libraries = []
for lib in libraries:
    if "**/*.jar" in lib:
        for root, _, files in os.walk("lib"):
            for file in files:
                if file.endswith(".jar"):
                    expanded_libraries.append(os.path.join(root, file))
    else:
        expanded_libraries.append(lib)

classpath = ";".join(expanded_libraries)

print("\nBibliothèques utilisées dans le classpath :")
for lib in expanded_libraries:
    print(lib)

if os.path.exists("jars/DarkQOL.jar"):
    os.remove("jars/DarkQOL.jar")
    print("Ancien DarkQOL.jar supprimé.")

""" if os.path.exists("bin"):
    os.system("rd /s /q bin")
    print("Dossier bin supprimé.") 

os.makedirs("bin", exist_ok=True)

    """

if os.path.exists("target"):
    os.system("rd /s /q target")
    print("Dossier target supprimé.")

# Get all Java files in src directory and subdirectories
java_files = glob.glob('src/**/*.java', recursive=True)

# Build the javac command
""" javac_command = (
    f'del ..\\DarkQOL.rar &&'
    f'"{javac_path}" -d bin -source 7 -target 7 -sourcepath src -cp "{classpath}" {" ".join(java_files)} && '
    f'"{jar_path}" cvf jars\\DarkQOL.jar -C bin . && '
    f'"{winrar_path}" a -r ..\\DarkQOL.rar ./**'
) """

javac_command = (
    f'del ..\\DarkQOL.rar &&'
    f'mvn clean compile package &&'
    f'"{winrar_path}" a -r ..\\DarkQOL.rar ./**'
)

""" javac_command = (
    f'del ..\\DarkQOL.rar &&'
    f'javac -d bin -source 21 -target 21 -sourcepath src -cp "{classpath}" {" ".join(java_files)} && '
    f'"{jar_path}" cvf jars\\DarkQOL.jar -C bin . && '
    f'"{winrar_path}" a -r ..\\DarkQOL.rar ./**'
)
 """
print("Commande générée :")
print(javac_command)

os.system(javac_command)
