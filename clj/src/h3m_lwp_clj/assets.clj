(ns h3m-lwp-clj.assets
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture]
   [com.badlogic.gdx.graphics.g2d TextureAtlas]
   [com.badlogic.gdx.assets AssetManager]
   [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
   [com.badlogic.gdx.utils Array])
  (:require
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.utils :as utils]))


(def ^AssetManager asset-manager
  (new AssetManager))


(defn file-exists?
  [file-path]
  (->> file-path
       (.local Gdx/files)
       (.exists)))


(defn init
  []
  (Texture/setAssetManager asset-manager)
  (when (file-exists? consts/atlas-file-name)
    (doto asset-manager
      (.load
       consts/atlas-file-name
       TextureAtlas
       (new TextureAtlasLoader$TextureAtlasParameter true))
      (.finishLoadingAsset consts/atlas-file-name)
      (.finishLoading))))


(defn get-def-frames
  ^Array
  [def-name]
  (let [^TextureAtlas atlas (.get asset-manager consts/atlas-file-name)
        ^Array frames (.findRegions atlas def-name)]
    (when (.isEmpty frames)
      (println (format "def %s not found in atlas" def-name)))
    frames))


(defn get-map-object-frames
  [filename]
  (utils/reduce-libgdx-array conj [] (get-def-frames filename)))


(defn get-terrain-sprite
  ^Array
  [def-name index]
  (let [terrain-frames (get-def-frames def-name)]
    (if (.isEmpty terrain-frames)
      terrain-frames
      (doto (new Array 1)
        (.add (.get terrain-frames index))))))


(defn assets-ready?
  []
  (file-exists? consts/atlas-file-name))