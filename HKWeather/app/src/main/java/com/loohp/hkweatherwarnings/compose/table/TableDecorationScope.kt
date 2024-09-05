/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.loohp.hkweatherwarnings.compose.table

/**
 * Collects the vertical/horizontal offsets of each row/column of a [DataTable] that are available
 * to a table decoration when its body is executed on a [TableDecorationScope] instance.
 */
interface TableDecorationScope {
    val verticalOffsets: List<Int>
    val horizontalOffsets: List<Int>
}

internal data class TableDecorationScopeImpl(
    override val verticalOffsets: List<Int>,
    override val horizontalOffsets: List<Int>
) : TableDecorationScope