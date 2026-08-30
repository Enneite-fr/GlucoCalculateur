# Plan d'implémentation : Import/Export CSV

Ce plan détaille l'ajout d'une fonctionnalité d'import et d'export au format CSV pour les aliments et les recettes, en remplacement du JSON pour l'usage utilisateur via tableur (Excel, Google Sheets, etc.).

## Propositions pour le format CSV

### 1. Aliments (Foods) - Format Strict 2 Colonnes
Comme demandé, le format sera simple et directement exploitable :
- **Colonne 1** : Nom de l'aliment
- **Colonne 2** : Taux de glucides (pour 100g)
- *Séparateur recommandé* : Point-virgule (`;`) car c'est le standard pour les tableurs configurés en français (évite les conflits avec la virgule des nombres décimaux).

### 2. Recettes (Recipes) - Format Adapté
Le CSV n'étant pas naturellement hiérarchique, je propose deux options pour les recettes tout en essayant de rester proche de votre souhait de simplicité :

#### Option A : Format hiérarchique (2 colonnes)
On utilise une ligne "marqueur" pour le début d'une recette, suivie de ses ingrédients.
| Colonne 1 | Colonne 2 |
| :--- | :--- |
| **[RECETTE] Ma Tarte** | |
| Farine | 200 |
| Sucre | 50 |
| **[RECETTE] Café au lait** | |
| Lait | 100 |

*Avantage : Respecte la contrainte de 2 colonnes. Facile à lire pour un humain.*

#### Option B : Format à plat (3 colonnes) - **Ma recommandation**
Même si vous avez évoqué 2 colonnes, pour les recettes, un format à plat est beaucoup plus simple à manipuler dans un tableur (pour faire des tris, des filtres ou des calculs).
| Nom Recette | Ingrédient | Poids (g) |
| :--- | :--- | :--- |
| Ma Tarte | Farine | 200 |
| Ma Tarte | Sucre | 50 |

### 3. Gestion intelligente des doublons
Pour l'import, l'application ne se contentera pas d'ajouter des lignes. Elle suivra cette logique :
1. **Recherche par nom** : Si un aliment (ou une recette) avec le même nom existe déjà.
2. **Mise à jour** : Les valeurs seront mises à jour avec celles du fichier importé (on considère que le fichier externe est la "source de vérité" lors d'un import).
3. **Création** : Si le nom est inconnu, un nouvel élément est créé.

## Changements proposés

### UI (Paramètres)
- [MODIFY] [SettingsScreen.kt](file:///F:/Git/Glucocalculateur/app/src/main/java/com/example/glucocalculateur/ui/screens/SettingsScreen.kt) : Remplacer les libellés JSON par CSV et mettre à jour les types MIME pour les sélecteurs de fichiers.

### Logique (ViewModel)
- [MODIFY] [GlucocalculateurViewModel.kt](file:///F:/Git/Glucocalculateur/app/src/main/java/com/example/glucocalculateur/ui/GlucocalculateurViewModel.kt) :
    - Ajouter `exportFoodsToCsv(uri)` et `importFoodsFromCsv(uri)`.
    - Ajouter `exportRecipesToCsv(uri)` et `importRecipesFromCsv(uri)`.
    - Implémenter le parsing CSV (gestion des guillemets, des séparateurs, et de l'encodage UTF-8).

### Données
- [MODIFY] [Daos.kt](file:///F:/Git/Glucocalculateur/app/src/main/java/com/example/glucocalculateur/data/Daos.kt) : Ajouter des méthodes de recherche par nom (`getFoodByName`, `getRecipeByName`) pour faciliter la détection des doublons.

## Questions Ouvertes
1. **Séparateur** : Préférez-vous la virgule `,` (standard international) ou le point-virgule `;` (standard français/Excel) ?
2. **Recettes** : L'option A (2 colonnes avec marqueur) vous convient-elle pour rester sur du "2 colonnes", ou l'option B (3 colonnes) vous semble-t-elle plus pratique pour vos modifications ?
3. **Fichier unique ou séparé** : Voulez-vous deux boutons séparés (Exporter Aliments / Exporter Recettes) ou un seul export qui génère deux fichiers (ou un fichier combiné avec des sections) ?

## Vérification
- Test d'import avec un fichier créé sur Excel (Windows) et Google Sheets.
- Vérification que la modification d'un taux de glucide dans le CSV met bien à jour l'aliment existant sans créer de doublon.
- Test de robustesse face aux noms contenant des caractères spéciaux ou des points-virgules.
