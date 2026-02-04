# **Où est ce fdp de tram ? 🚋💨**

Une application Android moderne et réactive pour suivre en temps réel les trams et bus de France. Fini de poireauter à l'arrêt sans savoir si ton tram est déjà passé ou s'il est bloqué !

## **✨ Fonctionnalités**

* 🗺️ **Carte Satellite immersive** : Une vue Google Maps en mode satellite pour mieux repérer les véhicules dans la ville.
* 📍 **Marqueurs Intelligents** :
    * Indiquent le numéro de ligne, la couleur officielle et l'orientation.
    * **Animation fluide** (Interpolation) : Les véhicules glissent sur la carte au lieu de se téléporter.
* ⚡ **Performance Extreme** :
    * Système de **View Caching** pour générer les marqueurs sans lag.
    * **Smart Redraw** : L'icône n'est régénérée que si le véhicule tourne réellement (> 2°), garantissant une map fluide même avec 50+ véhicules.
* 📑 **Détails du trajet** : En cliquant sur un véhicule, une BottomSheet s'ouvre pour afficher :
    * La destination finale.
    * La liste complète des arrêts à venir (avec défilement automatique pour les noms longs).
    * L'heure de passage prévue (théorique vs temps réel).
    * **Calcul du retard** (Code couleur vert/rouge).
    * Logos des réseaux (SNCF, Naolib, etc.) chargés dynamiquement.

## **🛠️ Stack Technique**

* **Langage** : Java
* **Réseau** : [Retrofit 2](https://square.github.io/retrofit/) + GSON pour la consommation de l'API Bus-Tracker.
* **Images** : [Glide](https://github.com/bumptech/glide) + Module SVG personnalisé pour les logos.
* **Maps** : Google Maps SDK for Android.
* **UI/UX** :
    * Material Design 3.
    * `ValueAnimator` pour les interpolations de mouvement.
    * `BottomSheetDialog` pour les détails.
* **Architecture** :
    * Gestion asynchrone pour ne jamais bloquer le Thread Principal (Main Thread).
    * Utilisation de `WeakReference` pour éviter les fuites de mémoire (Memory Leaks).

## **🛠️ Configuration & Installation**

### **Prérequis**

* Android Studio.
* Android Gradle Plugin (AGP) **8.7.3**.
* Compile SDK **35**.

### **Dépendances clés**

Le projet utilise des versions spécifiques pour garantir la compatibilité :

`implementation("androidx.activity:activity:1.9.3")`
`implementation("com.squareup.retrofit2:retrofit:2.11.0")`
`implementation("com.bumptech.glide:glide:4.16.0")`
`implementation("com.caverock:androidsvg-aar:1.4")`

### **Clé API Google Maps**

N'oublie pas d'ajouter ta clé API Google Maps dans ton fichier `local.properties` :

`MAPS_API_KEY=VOTRE_CLE_ICI`

## **📂 Structure du Projet**

* `MainActivity.java` : Orchestration principale, gestion de la Google Map, du cache des vues (Bitmap) et des animations `ValueAnimator`.
* `FetchingManager.java` : Le "cerveau" réseau. Gère les appels API asynchrones via Retrofit.
* `VehicleDetailsManager.java` : Contrôleur dédié à l'UI des détails. Gère la BottomSheet et le formatage des horaires avec sécurité anti-crash (WeakReference).
* `RouteArtist.java` : Gestionnaire de tracé des lignes (Polyline) avec parsing GeoJSON robuste et sécurisé.
* `NaolibApiService.java` : Définition des endpoints.
* `GlideSVGManager` : Pipeline de décodage pour afficher les logos vectoriels (.svg).

## **🤝 Crédits & Sources**

Cette application s'appuie sur le travail de **Kevin Biojout** et son projet [bus-tracker](https://github.com/kevinbioj/bus-tracker-2) qui agrège les données open-data des réseaux de transports français.

## **📝 À propos**

Développé par un étudiant qui en avait marre d'attendre son tram.