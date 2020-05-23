package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.app.KtxScreen
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private val h3mMap = H3mReader(Gdx.files.internal("maps/invasion.h3m").read()).read()
    private var terrainRenderer = TerrainRenderer(engine, h3mMap)
    private var objectsRenderer = ObjectsRenderer(engine, h3mMap)
    private var inputProcessor = InputProcessor(engine.camera).apply {
        onRandomizeCameraPosition = ::randomizeCameraPosition
    }
    private var mapUpdateInterval = DEFAULT_MAP_UPDATE_INTERVAL
    private var lastMapUpdateTime = System.currentTimeMillis()

    init {
        engine.camera.setToOrtho(true)
        applyPreferences()
        randomizeCameraPosition()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    private fun applyPreferences() {
        val prefs = Gdx.app
            .getPreferences(PREFERENCES_NAME)

        // Old float/integer preferences used in <= 2.2.0
        mapUpdateInterval = kotlin
            .runCatching {
                prefs
                    .getFloat(MAP_UPDATE_INTERVAL)
                    .also { prefs.putString(MAP_UPDATE_INTERVAL, it.toString()).flush() }
            }
            .recoverCatching { prefs.getString(MAP_UPDATE_INTERVAL).toFloat() }
            .map { if (it == 0f) DEFAULT_MAP_UPDATE_INTERVAL else it }
            .getOrDefault(DEFAULT_MAP_UPDATE_INTERVAL)

        val scale = kotlin
            .runCatching {
                prefs
                    .getInteger(SCALE)
                    .also { prefs.putString(SCALE, it.toString()).flush() }
            }
            .recoverCatching { prefs.getString(SCALE).toInt() }
            .map { if (it == 0) DEFAULT_SCALE else it }
            .getOrDefault(DEFAULT_SCALE)

        engine.camera.zoom = when (scale) {
            0 -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / scale.toFloat()
        }
    }

    private fun randomizeCameraPosition() {
        val camera = engine.camera

        val cameraViewportWidthTiles = ceil(camera.viewportWidth * camera.zoom / TILE_SIZE)
        val halfWidth = ceil(cameraViewportWidthTiles / 2).toInt()
        val nextCameraX = Random.nextInt(halfWidth, h3mMap.header.size - halfWidth) * TILE_SIZE

        val cameraViewportHeightTiles = ceil(camera.viewportHeight * camera.zoom / TILE_SIZE)
        val halfHeight = ceil(cameraViewportHeightTiles / 2).toInt()
        val nextCameraY = Random.nextInt(halfHeight, h3mMap.header.size - halfHeight) * TILE_SIZE

        camera.position.set(nextCameraX, nextCameraY, 0f)
        objectsRenderer.updateVisibleSprites()
    }

    override fun show() {
        applyPreferences()

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMapUpdateTime >= mapUpdateInterval) {
            lastMapUpdateTime = currentTime
            randomizeCameraPosition()
        }
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height, false)
    }

    override fun render(delta: Float) {
        engine.camera.update()
        terrainRenderer.render()
        objectsRenderer.render(delta)
    }

    override fun dispose() {
        super.dispose()
        terrainRenderer.dispose()
        objectsRenderer.dispose()
    }
}