# SitoTV 📺

Application IPTV personnelle pour **Android TV** et **Amazon Fire Stick**.  
Clone de SitoHomeTV avec fonctionnalités M3U complètes.

---

## Fonctionnalités

- ✅ **Ajout playlist M3U par URL** (http/https)
- ✅ **Ajout playlist M3U par fichier** (stockage local)
- ✅ **Xtream Codes** (serveur + username + password)
- ✅ Live TV, Films, Séries
- ✅ Filtrage par catégories
- ✅ Recherche de chaînes
- ✅ Player ExoPlayer (HLS, RTSP, TS, MP4)
- ✅ Interface optimisée télécommande D-PAD
- ✅ Compatible Android TV & Fire Stick

---

## Télécharger l'APK

> Les APK sont générés automatiquement par GitHub Actions à chaque `git push`.

1. Allez dans l'onglet **Releases** (colonne droite sur GitHub)
2. Cliquez sur la dernière version
3. Téléchargez `SitoTV-debug.apk` ou `SitoTV-release.apk`

---

## Installer sur Fire Stick

### Étape 1 — Activer les sources inconnues
1. Fire Stick → **Paramètres** → Mon Fire TV → **Options pour développeurs**
2. Activez **Applications de sources inconnues**

### Étape 2 — Installer l'app Downloader
1. Cherchez **Downloader** dans l'App Store Fire Stick
2. Installez-la (c'est gratuit)

### Étape 3 — Installer SitoTV
1. Ouvrez **Downloader**
2. Entrez l'URL directe de l'APK depuis GitHub Releases
3. Téléchargez et installez

---

## Construire localement (Android Studio)

```bash
git clone https://github.com/VOTRE_USER/SitoTV.git
cd SitoTV
./gradlew assembleDebug
# APK dans : app/build/outputs/apk/debug/
```

---

## Usage

1. Lancez SitoTV
2. Cliquez **Playlists** → **+ Ajouter**
3. Choisissez le type :
   - **Lien M3U** : collez votre URL M3U
   - **Fichier M3U** : sélectionnez un fichier `.m3u` / `.m3u8`
   - **Xtream Codes** : entrez serveur + identifiants
4. Naviguez avec Live TV / Films / Séries
