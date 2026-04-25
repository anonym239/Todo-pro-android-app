# TodoPro – Native Android App

Eine native Android Todo-App mit Dark/Light Theme, Erinnerungen (auch bei gesperrtem Bildschirm), Konfetti-Animation und mehr.

---

## 🚀 APK bauen mit Android Studio

### Schritt 1: Android Studio installieren
1. Gehe zu https://developer.android.com/studio
2. Lade Android Studio herunter und installiere es
3. Beim ersten Start: **Standard Setup** wählen → alles automatisch installieren lassen (SDK, etc.)

### Schritt 2: Projekt öffnen
1. Android Studio starten
2. **"Open"** klicken
3. Den Ordner `c:\Users\Benutze\Desktop\test` auswählen
4. Warten bis Gradle sync fertig ist (unten in der Statusleiste)

### Schritt 3: APK bauen
1. Oben im Menü: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Warten bis der Build fertig ist
3. Unten rechts erscheint: **"APK(s) generated successfully"** → auf **"locate"** klicken
4. Die APK liegt in: `app\build\outputs\apk\debug\app-debug.apk`

---

## 📱 APK auf Android 16 installieren

### Option A: Per USB-Kabel
1. Handy per USB anschließen
2. Am Handy: **Einstellungen → Über das Telefon → Build-Nummer** 7x tippen (Entwickleroptionen aktivieren)
3. **Einstellungen → Entwickleroptionen → USB-Debugging** aktivieren
4. In Android Studio: oben das Gerät auswählen → **Run** (grüner Play-Button)

### Option B: APK direkt übertragen
1. APK-Datei per USB/WhatsApp/Google Drive aufs Handy übertragen
2. Am Handy: **Einstellungen → Apps → Spezielle App-Zugriffe → Unbekannte Apps installieren**
3. Den Dateimanager/Browser erlauben
4. APK öffnen und installieren

---

## ⚠️ Wichtig für zuverlässige Benachrichtigungen (Android 12+)

Nach der Installation:
1. **Einstellungen → Apps → TodoPro → Benachrichtigungen** → Alle erlauben
2. **Einstellungen → Apps → TodoPro → Akku** → "Nicht optimieren" wählen
3. Beim ersten Start der App: Dialog "Exakte Alarme erlauben" → **"Einstellungen öffnen"** tippen und erlauben

---

## 📤 APK auf GitHub hochladen

### Schritt 1: GitHub Account erstellen
1. Gehe zu https://github.com
2. Registriere dich (kostenlos)

### Schritt 2: Neues Repository erstellen
1. Auf GitHub: **"New repository"** klicken (grüner Button)
2. Name eingeben z.B. `TodoPro`
3. **"Public"** oder **"Private"** wählen
4. **"Create repository"** klicken

### Schritt 3: Git einrichten (einmalig)
Öffne die **Eingabeaufforderung** (cmd) und tippe:
```
git config --global user.name "DeinName"
git config --global user.email "deine@email.com"
```

### Schritt 4: Projekt zu GitHub pushen
```
cd c:\Users\Benutze\Desktop\test
git init
git add .
git commit -m "TodoPro App - erste Version"
git branch -M main
git remote add origin https://github.com/DEIN-USERNAME/TodoPro.git
git push -u origin main
```
*(DEIN-USERNAME durch deinen GitHub-Benutzernamen ersetzen)*

### Schritt 5: APK als Release hochladen
1. Auf GitHub in deinem Repository: **"Releases"** → **"Create a new release"**
2. **"Choose a tag"** → `v1.0` eingeben → **"Create new tag"**
3. Titel: `TodoPro v1.0`
4. Unter **"Attach binaries"**: die `app-debug.apk` Datei hochziehen
5. **"Publish release"** klicken

✅ Jetzt kann jeder deine APK unter:
`https://github.com/DEIN-USERNAME/TodoPro/releases` herunterladen!

---

## 🎯 App-Features

| Feature | Beschreibung |
|---------|-------------|
| ✅ Aufgaben | Hinzufügen, bearbeiten, löschen |
| 🔔 Erinnerungen | Auch bei geschlossener App & gesperrtem Bildschirm |
| ⚡ Schnell-Timer | In 15 Min, 30 Min, 1 Stunde, Morgen 9:00 |
| 🚨 Priorität | Mit `!` am Anfang → wird oben angeheftet |
| 🎉 Konfetti | Bei erledigten Aufgaben |
| 🌙 Dark/Light | Theme-Toggle oben rechts |
| 💾 Autosave | Automatisch lokal gespeichert |
| 🔄 Boot-Restore | Alarme werden nach Neustart wiederhergestellt |

---

## 📁 Projektstruktur

```
app/src/main/
├── java/com/todopro/app/
│   ├── MainActivity.kt       ← Hauptbildschirm
│   ├── TodoAdapter.kt        ← RecyclerView Adapter
│   ├── TodoItem.kt           ← Datenmodell
│   ├── TodoStorage.kt        ← SharedPreferences
│   ├── AlarmScheduler.kt     ← Alarm setzen/löschen
│   ├── AlarmReceiver.kt      ← Benachrichtigung anzeigen
│   └── BootReceiver.kt       ← Alarme nach Neustart
└── res/
    ├── layout/               ← XML Layouts
    ├── drawable/             ← Icons & Shapes
    └── values/               ← Farben, Styles, Strings
```
