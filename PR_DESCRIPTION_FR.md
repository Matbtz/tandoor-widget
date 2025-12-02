# Améliorations des Workflows CI/CD

## Contexte du Problème

Le dépôt Matbtz/tandoor-widget présentait deux problèmes principaux :

1. **Échec du déclenchement du workflow release** : Le workflow auto-tag créait le tag mais le workflow release ne se déclenchait pas. Les logs montraient une erreur 403 lors du dispatch API : "Resource not accessible by personal access token".

2. **Échecs de build Gradle** : Le build APK échouait parfois avec l'erreur "Could not find or load main class org.gradle.wrapper.GradleWrapperMain" - le Gradle Wrapper JAR était absent ou non trouvé sur le runner.

## Solution Implémentée

### 1. Modifications de `.github/workflows/auto-tag-on-merge.yml`

**Améliorations apportées :**

- ✅ **Logging HTTP détaillé** : Capture et affiche les codes de réponse HTTP et le corps des réponses pour toutes les API calls
- ✅ **Mécanisme de fallback** : Si le dispatch API échoue (403 ou autre erreur), le workflow utilise maintenant un fallback via git push
- ✅ **Sécurité renforcée** : Utilisation de git credential helper pour éviter l'exposition du token dans les logs
- ✅ **Messages d'erreur clairs** : Guide les mainteneurs sur les actions à entreprendre en cas d'échec

**Flux de travail :**
1. Crée le tag via l'API GitHub en utilisant `REPO_PAT`
2. Tente de déclencher `release.yml` via l'API workflow dispatch
3. Si le dispatch échoue (erreur 403 par exemple), bascule vers un push git du tag avec authentification
4. Le push git déclenche le workflow release via le trigger existant `on: push: tags:`

### 2. Modifications de `.github/workflows/release.yml`

**Améliorations apportées :**

- ✅ **Ajout de `gradle/gradle-build-action@v3`** : Cette action garantit que Gradle est correctement configuré même si le wrapper est manquant ou corrompu
- ✅ **Validation du Gradle Wrapper** : Étape de validation qui vérifie gradlew et gradle-wrapper.jar avec des messages informatifs
- ✅ **Build robuste** : Le gradle-build-action gère automatiquement les problèmes de wrapper JAR manquant
- ✅ **Meilleur debugging** : Ajout du flag `--stacktrace` pour une meilleure diagnostique des erreurs

**Note :** Le trigger `workflow_dispatch` était déjà présent, aucune modification n'était nécessaire.

### 3. Documentation complète

Création de `CI_WORKFLOW_IMPROVEMENTS.md` (en anglais) contenant :
- Explication détaillée des changements
- Guide de dépannage
- Actions requises pour les mainteneurs
- Notes de sécurité

## Actions Requises pour les Mainteneurs

### ⚠️ Vérifier le Secret REPO_PAT

Le workflow utilise un secret appelé `REPO_PAT` (Personal Access Token). Veuillez vous assurer que :

1. **Le secret existe** : Vérifier dans Settings → Secrets and variables → Actions
2. **Permissions requises** : Le PAT doit avoir les scopes suivants :
   - ✅ `repo` (Contrôle total des dépôts privés)
   - ✅ `workflow` (Mise à jour des workflows GitHub Actions) - optionnel mais recommandé
3. **Autorisation SSO** : Si ce dépôt fait partie d'une organisation avec SSO activé :
   - Aller dans GitHub Settings → Developer settings → Personal access tokens
   - Trouver le token utilisé pour REPO_PAT
   - Cliquer sur "Configure SSO" → "Authorize" pour l'organisation

### ✅ Vérifier les Autres Secrets

S'assurer que ces secrets sont toujours présents et valides pour la signature de l'APK :
- `KEYSTORE_BASE64` : Fichier keystore encodé en base64
- `KEYSTORE_PASSWORD` : Mot de passe du keystore
- `KEY_ALIAS` : Alias de la clé dans le keystore
- `KEY_PASSWORD` : Mot de passe de la clé

## Tests Effectués

✅ **Validation syntaxique YAML** : Les deux fichiers workflow ont été validés avec yamllint et PyYAML
✅ **Analyse de sécurité CodeQL** : Aucune vulnérabilité détectée
✅ **Code review** : Feedback de sécurité adressé (protection du token dans les logs)

## Tests à Effectuer Après Merge

1. **Test du workflow auto-tag** : Merger une PR et vérifier que :
   - Le tag est créé avec succès
   - Le workflow release est déclenché (via dispatch ou fallback git push)
   - Les logs montrent clairement les codes de réponse HTTP

2. **Test du workflow release** : Déclencher manuellement le workflow :
   - Aller dans Actions → "Build and Release APK" → "Run workflow"
   - Sélectionner un tag ou une branche
   - Vérifier que l'APK se build et la release est créée avec succès

## Dépannage

**Si vous voyez une erreur 403 dans les logs :**

1. Vérifier la réponse du workflow dispatch dans les logs (code HTTP et corps de réponse)
2. Vérifier que le fallback a fonctionné (chercher le message "Attempting fallback: pushing tag via git")
3. Vérifier les permissions du PAT (scopes `repo` et `workflow`)
4. Vérifier l'autorisation SSO si dans une organisation SSO

**Si le build Gradle échoue :**

1. Vérifier que les fichiers wrapper existent : `gradle/wrapper/gradle-wrapper.jar`
2. Consulter les logs : l'étape de validation indiquera ce qui manque
3. Le gradle-build-action devrait gérer automatiquement les wrapper JAR manquants

## Notes de Sécurité

🔒 **Aucun token exposé** : Les valeurs des PAT ne sont jamais exposées dans les logs
🔒 **Authentification sécurisée** : Toutes les authentifications utilisent GitHub Secrets
🔒 **Pas de nouveaux secrets** : Aucun secret n'a été ajouté ou modifié par cette PR
🔒 **Mécanisme de fallback sécurisé** : Utilise le même REPO_PAT secret, juste via git au lieu de l'API

## Fichiers Modifiés

1. `.github/workflows/auto-tag-on-merge.yml` (+60 lignes)
   - Ajout de logging HTTP détaillé
   - Implémentation du mécanisme de fallback
   - Sécurisation de l'authentification git

2. `.github/workflows/release.yml` (+22 lignes)
   - Intégration de gradle-build-action
   - Validation du wrapper Gradle
   - Amélioration du debugging

3. `CI_WORKFLOW_IMPROVEMENTS.md` (nouveau fichier, 97 lignes)
   - Documentation complète en anglais
   - Guide de dépannage
   - Références aux ressources GitHub

## Références

- [GitHub API: Workflow Dispatches](https://docs.github.com/en/rest/actions/workflows#create-a-workflow-dispatch-event)
- [gradle-build-action Documentation](https://github.com/gradle/gradle-build-action)
- [GitHub Personal Access Tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

---

**Cette PR est prête à être mergée.** Une fois mergée, tester avec une nouvelle PR pour confirmer que tout fonctionne correctement.
