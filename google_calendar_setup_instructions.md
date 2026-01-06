# Instructions de configuration pour la synchronisation avec Google Agenda

### Fonctionnalités implémentées

*   **Bouton 'S'inscrire'**: Un bouton 'S'inscrire' a été ajouté à chaque carte d'événement.
*   **Sauvegarde des événements**: Lorsque l'utilisateur clique sur 'S'inscrire', l'ID de l'événement est enregistré dans son profil Firebase.
*   **Synchronisation avec Google Agenda**: Après l'enregistrement dans Firebase, l'application demandera à l'utilisateur de se connecter à son compte Google et d'accorder les permissions nécessaires pour accéder à son Google Agenda. Une fois les permissions accordées, l'événement sera ajouté à son agenda Google principal.
*   **État du bouton**: Le bouton 'S'inscrire' sera désactivé pour les événements déjà enregistrés.

### Étapes de configuration (Google Cloud Console)

Pour que la synchronisation avec Google Agenda fonctionne, vous devez configurer votre projet Google Cloud :

1.  **Activer l'API Google Calendar**:
    *   Rendez-vous sur la [Google Cloud Console](https://console.cloud.google.com/).
    *   Sélectionnez votre projet (ou créez-en un nouveau).
    *   Dans le menu de navigation, allez dans 'APIs & Services' > 'Bibliothèque'.
    *   Recherchez 'Google Calendar API' et activez-la.

2.  **Configurer l'écran de consentement OAuth**:
    *   Dans le menu de navigation, allez dans 'APIs & Services' > 'Écran de consentement OAuth'.
    *   Choisissez 'Externe' et 'Créer'.
    *   Renseignez les informations de l'application (nom, e-mail de support, etc.).
    *   Ajoutez le scope suivant : `https://www.googleapis.com/auth/calendar`.

3.  **Créer un ID client OAuth 2.0 pour Android**:
    *   Dans le menu de navigation, allez dans 'APIs & Services' > 'Identifiants'.
    *   Cliquez sur '+ Créer des identifiants' et sélectionnez 'ID client OAuth'.
    *   Choisissez 'Android' comme type d'application.
    *   Entrez le nom du package de votre application : `com.example.coursemanagment`.
    *   Ajoutez l'empreinte du certificat SHA-1 de votre application. Pour l'obtenir, exécutez la commande suivante dans votre terminal :
        ```bash
        keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -list -v
        ```
    *   Cliquez sur 'Créer'.

Une fois ces étapes de configuration terminées, la fonctionnalité de synchronisation des événements avec Google Agenda sera opérationnelle.
