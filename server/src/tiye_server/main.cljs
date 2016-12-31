
(ns tiye-server.main
  (:require [cljs.nodejs :as nodejs]
            [tiye-server.schema :as schema]
            [tiye-server.network :refer [run-server! render-clients!]]
            [tiye-server.updater.core :refer [updater]]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce writer-db-ref (atom schema/database))

(defonce reader-db-ref (atom @writer-db-ref))

(defn render-loop! []
  (if (not= @reader-db-ref @writer-db-ref)
    (do (reset! reader-db-ref @writer-db-ref) (render-clients! @reader-db-ref)))
  (js/setTimeout render-loop! 300))

(defn -main []
  (nodejs/enable-util-print!)
  (let [server-ch (run-server! {:port 5020})]
    (go-loop
     []
     (let [[op op-data state-id op-id op-time] (<! server-ch)]
       (try
        (let [new-db (updater @writer-db-ref op op-data state-id op-id op-time)]
          (reset! writer-db-ref new-db))
        (catch js/Error e (.log js/console e)))
       (recur)))
    (render-loop!))
  (add-watch writer-db-ref :log (fn [] ))
  (println "server started"))

(defn rm-caches! [] (.execSync (js/require "child_process") "rm .lumo_cache/tiye*"))

(defn on-jsload! [] (println "Code updated.") (render-clients! @reader-db-ref))
