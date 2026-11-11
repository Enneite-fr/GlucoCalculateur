# Publier GlucoCalculateur

Identifiant de l'application : `fr.enneite.glucocalculateur` (définitif une fois publié sur le Play Store).

Ce guide comporte deux parties : une **mise en place unique**, puis la **routine de mise à jour** (quelques minutes).

---

## 1. Mise en place (une seule fois)

### 1.1 Créer la clé d'envoi (upload key)

Dans un terminal, **hors du dossier du projet** (la clé ne doit jamais être versionnée) :

```bash
keytool -genkeypair -v -keystore glucocalculateur-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` demande un mot de passe et quelques informations (nom, pays…). Placez le fichier `glucocalculateur-upload.jks` **à côté** du dossier `GlucoCalculateur` (par exemple `F:\Git\glucocalculateur-upload.jks`).

> **Sauvegardez ce fichier et son mot de passe** (gestionnaire de mots de passe, clé USB). Avec la signature
> d'applications Play (activée par défaut), une clé d'envoi perdue peut être réinitialisée par Google, mais c'est
> une procédure de plusieurs jours.

### 1.2 Signature locale

Copiez `keystore.properties.example` en `keystore.properties` (à la racine du projet) et complétez les mots de passe. Ce fichier est ignoré par git. `storeFile` est relatif à la racine du projet : `../glucocalculateur-upload.jks` si la clé est à côté du dossier, `glucocalculateur-upload.jks` si elle est dedans (les fichiers `.jks` sont ignorés par git).

Si la signature n'est pas configurée, la construction de la release s'arrête avec un message explicite : un APK non signé (`app-release-unsigned.apk`) ne s'installe sur aucun téléphone.

Vérification :

```bash
./gradlew bundleRelease assembleRelease
```

Fichiers produits :
- `app/build/outputs/bundle/release/app-release.aab` : à envoyer sur le Play Store ;
- `app/build/outputs/apk/release/app-release.apk` : à installer directement sur un téléphone.

Android Studio peut aussi construire ces fichiers : *Build → Generate Signed App Bundle / APK*, avec la même clé.

### 1.3 Publication automatique via GitHub (facultatif mais recommandé)

Dans le dépôt GitHub : *Settings → Secrets and variables → Actions → New repository secret*. Créez :

| Secret | Valeur |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | le fichier `.jks` encodé en base64 (voir ci-dessous) |
| `SIGNING_STORE_PASSWORD` | mot de passe du fichier |
| `SIGNING_KEY_ALIAS` | `upload` |
| `SIGNING_KEY_PASSWORD` | mot de passe de la clé |

Encoder la clé (PowerShell) puis coller le contenu du presse-papiers dans le secret :

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("F:\Git\glucocalculateur-upload.jks")) | Set-Clipboard
```

### 1.4 Compte Play Console et fiche de l'application

1. Créer un compte développeur sur <https://play.google.com/console> (frais unique de 25 $, vérification d'identité).
2. *Créer une application* : nom « GlucoCalculateur », gratuite, langue par défaut français.
3. Remplir les déclarations demandées par la console :
   - **Règles de confidentialité** : URL de [PRIVACY.md](PRIVACY.md) sur GitHub
     (`https://github.com/Enneite-fr/GlucoCalculateur/blob/master/PRIVACY.md`) ;
   - **Sécurité des données** : aucune donnée collectée ni partagée ; données stockées uniquement sur l'appareil ;
   - **Classification du contenu**, **public cible** (adultes), **accès à l'application** (aucun identifiant requis),
     **publicités** (aucune) ;
   - catégorie suggérée : *Santé et remise en forme* ou *Médecine*. Une application de calcul de glucides n'est pas
     un dispositif médical, mais la console peut poser des questions sur les « applications de santé » : y répondre
     en précisant qu'il s'agit d'un outil de calcul, sans diagnostic ni conseil médical.
4. Fiche Play Store : description courte et longue, icône 512 × 512 (`app/src/main/ic_launcher-playstore.png`),
   image de présentation 1024 × 500, au moins deux captures d'écran de téléphone.
5. **Comptes personnels récents** : Google impose un **test fermé avec au moins 12 testeurs pendant 14 jours**
   avant de pouvoir publier en production. Commencer par la piste *Test fermé* (ou *Test interne* pour vous seul).

---

## 2. Publier une mise à jour

1. Dans `app/build.gradle.kts`, augmenter `versionCode` de 1 et mettre à jour `versionName` (ex. `1.1.0` → `1.2.0`).
2. Commiter, puis créer et pousser le tag correspondant :

   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

3. GitHub Actions (workflow *Release*) lance les tests, construit l'APK et l'AAB signés, crée la release GitHub
   avec l'APK, et met l'AAB à disposition dans l'onglet *Actions* (artefact `play-store-bundle`).
4. Play Console : *Tests et publication → (piste choisie) → Créer une version*, déposer le `.aab`, enregistrer, publier.

Sans GitHub Actions : `./gradlew bundleRelease assembleRelease` en local, puis mêmes étapes avec les fichiers produits.

---

## 3. Passer de l'ancienne application à la nouvelle

Le changement d'identifiant (`com.example.glucocalculateur` → `fr.enneite.glucocalculateur`) fait de la version
publiée une **nouvelle application** : elle s'installe à côté de l'ancienne, sans en reprendre les données.

1. Dans l'ancienne application (version de test de la branche `optimisation` ou ultérieure) :
   *Paramètres → Sauvegarde complète (JSON) → Exporter*. Ce fichier contient aliments, recettes **et repas**.
2. Installer la nouvelle application, puis *Paramètres → Sauvegarde complète (JSON) → Importer* ce fichier.
3. Vérifier les repas, puis désinstaller l'ancienne application.

Les versions antérieures à la branche `optimisation` n'exportent pas les repas : seuls les aliments et recettes
peuvent alors être repris.

La version de développement (`debug`) porte l'identifiant `fr.enneite.glucocalculateur.debug` et s'installe à côté
de la version publiée, sans la remplacer.
