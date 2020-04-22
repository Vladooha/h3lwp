package com.heroes3.livewallpaper.core

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.utils.Array
import com.heroes3.livewallpaper.core.JsonMapParser.ParsedMap
import com.heroes3.livewallpaper.core.JsonMapParser.TerrainTile
import ktx.collections.map
import java.util.*

class TerrainRenderer(
    private val assets: Assets,
    private val camera: OrthographicCamera,
    private val h3mMap: ParsedMap
) {
    private var tiledMap = TiledMap()
    private val renderer = OrthogonalTiledMapRenderer(tiledMap)

    init {
        createLayers()
    }

    class TileSettings(
        val frames: Array<AtlasRegion>,
        val flipX: Boolean,
        val flipY: Boolean
    )

    object Bits {
        fun convert(input: Int): BitSet {
            return convert(input.toLong())
        }

        fun convert(input: Long): BitSet {
            var value = input
            val bits = BitSet()
            var index = 0
            while (value != 0L) {
                if (value % 2L != 0L) {
                    bits.set(index)
                }
                ++index
                value = value ushr 1
            }
            return bits
        }

        fun convert(bits: BitSet): Long {
            var value = 0L
            for (i in 0 until bits.length()) {
                value += if (bits[i]) 1L shl i else 0L
            }
            return value
        }
    }

    private fun getTileSettingsByType(tile: TerrainTile, type: String): TileSettings {
        return when (type) {
            "terrain" -> TileSettings(
                assets.getTerrainFrames(Constants.TerrainDefs.byInt(tile.terrain), tile.terrainImageIndex),
                Bits.convert(tile.mirrorConfig).get(0),
                Bits.convert(tile.mirrorConfig).get(1)
            )
            "river" -> TileSettings(
                assets.getTerrainFrames(Constants.RiverDefs.byInt(tile.river), tile.riverImageIndex),
                Bits.convert(tile.mirrorConfig).get(2),
                Bits.convert(tile.mirrorConfig).get(3)
            )
            "road" -> TileSettings(
                assets.getTerrainFrames(Constants.RoadDefs.byInt(tile.road), tile.roadImageIndex),
                Bits.convert(tile.mirrorConfig).get(4),
                Bits.convert(tile.mirrorConfig).get(5)
            )
            else -> throw Exception("Incorrect tile settings")
        }
    }

    private fun createMapTile(frames: Array<AtlasRegion>): TiledMapTile {
        return if (frames.size > 1) {
            AnimatedTiledMapTile(0.18f, frames.map { StaticTiledMapTile(it) })
        } else {
            StaticTiledMapTile(frames.first())
        }
    }

    private fun createTile(tile: TerrainTile, type: String): TiledMapTileLayer.Cell {
        val settings = getTileSettingsByType(tile, type)
        val cell = TiledMapTileLayer.Cell()
        cell.tile = createMapTile(settings.frames)
        cell.flipHorizontally = settings.flipX
        cell.flipVertically = settings.flipY
        return cell
    }

    private fun createLayers() {
        val terrainLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, 32, 32)
        val riverLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, 32, 32)
        val roadLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, 32, 32)

        roadLayer.offsetY = -16f

        for (x in 0 until h3mMap.size) {
            for (y in 0 until h3mMap.size) {
                val index = h3mMap.size * y + x
                val tile = h3mMap.terrain[index]
                terrainLayer.setCell(x, y, createTile(tile, "terrain"))
                if (tile.river > 0) {
                    riverLayer.setCell(x, y, createTile(tile, "river"))
                }
                if (tile.road > 0) {
                    roadLayer.setCell(x, y, createTile(tile, "road"))
                }
            }
        }

        tiledMap.layers.add(terrainLayer)
        tiledMap.layers.add(riverLayer)
        tiledMap.layers.add(roadLayer)
    }

    fun render() {
        renderer.setView(camera)
        renderer.render()
    }
}