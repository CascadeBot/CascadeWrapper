package org.cascadebot.cascadewrapper;

import org.cascadebot.cascadewrapper.runnables.OperationRunnable;
import spark.Spark;

public class Web {

    public Web() {
        init();
    }

    public void init() {
        Spark.port(8081);

        Spark.path("/bot", () -> {
            Spark.get("/process", (req, res) -> { //Info on restarts / status
                res.type("application/json");
                return "";
            });

            Spark.get("/stats", (req, res) -> { //Stuff like cpu usage (might merge this with the other one)
                res.type("application/json");
                return Wrapper.GSON.toJson(OperationRunnable.getInstance().getManager().getProcessStats());
            });

            Spark.get("/logs", (req, res) -> { //Logs!
                res.type("application/json");
                return "";
            });
        });
    }
}
