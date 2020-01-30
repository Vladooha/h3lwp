(ns h3m-lwp-clj.desktop
  (:import
   [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
   [com.heroes3.livewallpaper.clojure LiveWallpaperEngine]
   [java.awt FileDialog Frame]))


(def ^FileDialog file-chooser
  (doto (new FileDialog (new Frame))
    (.setMode  FileDialog/LOAD)
    (.setFile "*.lod")
    (.setMultipleMode false)))


(defn create-engine
  ^LiveWallpaperEngine
  []
  (let [engine (new LiveWallpaperEngine)]
    (.onFileSelectClick
     engine
     (reify
       Runnable
       (run [_]
         (.show file-chooser)
         (let [file (.getFile file-chooser)
               directory (.getDirectory file-chooser)
               file-path (format "%s%s" directory file)]
           (.setFilePath engine file-path)))))
    engine))


(defn -main []
  (let [engine (create-engine)]
    (doto (new LwjglApplication engine)
      (.postRunnable
       (reify Runnable
         (run [_] (.setIsPreview engine true)))))))