package com.example.delitrack.backEnd.data

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

object MapTilerConfig {

    // Default OpenStreetMap Mapnik Tile Source (Free, No Key Required)
    fun getDefaultOpenStreetMapSource(): OnlineTileSourceBase {
        return TileSourceFactory.MAPNIK
    }

    // MapTiler Vector/Raster Tile Source Integration
    fun getMapTilerTileSource(apiKey: String = "YOUR_MAPTILER_API_KEY"): OnlineTileSourceBase {
        return object : OnlineTileSourceBase(
            "MapTilerStreets",
            0, 19, 256, ".png",
            arrayOf("https://api.maptiler.com/maps/streets-v2/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + ".png?key=" + apiKey
            }
        }
    }
}
