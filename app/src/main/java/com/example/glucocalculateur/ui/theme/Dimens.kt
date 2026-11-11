package com.example.glucocalculateur.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espacements et dimensions communs. */
object Dimens {
    val spaceXs = 4.dp
    val spaceS = 8.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXl = 24.dp

    /** Marge latérale des écrans. */
    val screenPadding = 16.dp

    /** Barre de navigation flottante (et bouton d'action, aligné sur sa hauteur). */
    val bottomBarHeight = 72.dp
    val bottomBarMargin = 16.dp

    /** Espace libre en bas des listes pour ne rien masquer sous la barre flottante. */
    val listBottomSpace = bottomBarHeight + bottomBarMargin * 2

    val minTouchTarget = 48.dp
}

/** Marge basse des listes : barre flottante + barre de navigation système. */
@Composable
fun listBottomPadding(): Dp =
    Dimens.listBottomSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
