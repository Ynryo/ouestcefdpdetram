# **Où est ce fdp de tram ? 🚋💨**

Une application Android moderne et réactive pour suivre en temps réel les trams et bus de France. Fini de poireauter à l'arrêt sans savoir si ton tram est déjà passé ou s'il est bloqué\!

## **✨ Fonctionnalités**

* 🗺️ **Carte Satellite immersive** : Une vue Google Maps en mode satellite pour mieux repérer les véhicules dans la ville.  
* 📍 **Marqueurs Dynamiques** : Les marqueurs indiquent le numéro de la ligne, la couleur officielle du réseau et **l'orientation réelle** (bearing) du véhicule grâce à une flèche intégrée.  
* 🔄 **Live Tracking** : Rafraîchissement automatique des positions toutes les 5 secondes sans clignotement de l'interface.  
* 📑 **Détails du trajet** : En cliquant sur un véhicule, une BottomSheet s'ouvre pour afficher :  
  * La destination finale.  
  * La liste complète des arrêts à venir.  
  * L'heure de passage prévue (théorique vs temps réel).  
  * **Calcul automatique du retard** (affichage en rouge/bleu).  
  * Logos des réseaux (SNCF, Naolib, etc.) chargés dynamiquement.

## **🛠️ Stack Technique**

* **Langage** : Java
* **Réseau** : [Retrofit 2](https://square.github.io/retrofit/) \+ GSON pour la consommation de l'API Naolib/Bus-Tracker.
* **Images** : [Glide](https://github.com/bumptech/glide) pour le chargement et le cache des logos de réseaux.
* **Maps** : Google Maps SDK for Android.
* **UI/UX** : Material Design 3 (Dynamic Colors), BottomSheetDialog, Custom Marker Layouts.  
* **Architecture** : Gestion asynchrone via des Listeners personnalisés pour éviter de bloquer le thread principal.

## **🛠️ Configuration & Installation**

### **Prérequis**

* Android Studio Ladybug (ou plus récent).
* Android Gradle Plugin (AGP) **8.7.3**.
* Compile SDK **35**.

### **Dépendances clés**

Le projet utilise des versions spécifiques pour garantir la compatibilité avec AGP 8.7.3 :

`implementation("androidx.activity:activity:1.9.3")`
`implementation("androidx.core:core:1.15.0")`
`implementation("com.squareup.retrofit2:retrofit:2.11.0")`
`implementation("com.bumptech.glide:glide:4.16.0")`

### **Clé API Google Maps**

N'oublie pas d'ajouter ta clé API Google Maps dans ton fichier `local.properties` :

`MAPS_API_KEY=VOTRE_CLE_ICI`

## **📂 Structure du Projet**

* `MainActivity.java` : Coeur de l'app, gestion de la carte et de la boucle de rafraîchissement.
* `FetchingManager.java` : Le "cerveau" réseau. Gère les appels API asynchrones et communique les résultats via des interfaces.
* `VehicleDetailsActivity.java` : Gère toute la logique d'affichage de la BottomSheet et le calcul des retards.
* `NaolibApiService.java` : Définition des endpoints Retrofit.
* `MarkerData.java` / `VehicleDetails.java` : Modèles de données (POJO) pour le parsing JSON.

## **🤝 Crédits & Sources**

Cette application s'appuie sur le travail de **Kevin Biojout** et son projet [bus-tracker](https://github.com/kevinbioj/bus-tracker-2) qui agrège les données open-data des réseaux de transports français.

## **📝 À propos**

Développé par un étudiant qui en avait marre d'attendre son tram. 🏳️‍🌈