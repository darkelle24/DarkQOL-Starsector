import sys
from PIL import Image
import os

def convert_and_resize_image(input_path, output_path, size=(190, 95)):
    try:
        img = Image.open(input_path)
        
        img = img.convert("RGBA")
        
        img = img.resize(size)
        
        img.save(output_path, format="PNG")

        print(f"Image sauvegardée en {output_path}")
    except Exception as e:
        print(f"Erreur : {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage : python script.py chemin/vers/image")
        sys.exit(1)
    
    input_image_path = sys.argv[1]
    
    if not os.path.isfile(input_image_path):
        print("Erreur : Le fichier spécifié n'existe pas.")
        sys.exit(1)
    
    output_image_path = os.path.splitext(input_image_path)[0] + "_resized.png"

    convert_and_resize_image(input_image_path, output_image_path)
