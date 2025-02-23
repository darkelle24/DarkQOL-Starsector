import json
import os

# Charger le fichier settings.json
settings_path = "./.vscode/settings.json"  # Remplace par le chemin de ton fichier si besoin
with open(settings_path, "r", encoding="utf-8") as f:
    print(f"Chargement du fichier {settings_path}")
    settings = json.load(f)

# Extraire les chemins des bibliothèques
libraries = settings.get("java.project.referencedLibraries", [])

# Remplacer les chemins globaux **/*.jar par les fichiers trouvés
expanded_libraries = []
for lib in libraries:
    if "**/*.jar" in lib:
        # Chercher tous les fichiers .jar dans le dossier "lib" (recursivement)
        for root, _, files in os.walk("lib"):
            for file in files:
                if file.endswith(".jar"):
                    expanded_libraries.append(os.path.join(root, file))
    else:
        expanded_libraries.append(lib)

# Construire le classpath sous Windows (séparateur `;`)
classpath = ";".join(expanded_libraries)

# Construire la commande finale
javac_command = (
    f'del ..\\DarkQOL.rar && '
    f'javac -d bin -source 8 -target 8 -sourcepath src -cp "{classpath}" src\\darkqol\\*.java && '
    f'jar cvf jars\\DarkQOL.jar -C bin . && '
    f'"C:\\Program Files\\WinRAR\\Rar.exe" a -r ..\\DarkQOL.rar ./**'
)


print("Commande générée :")
print(javac_command)

os.system(javac_command)
