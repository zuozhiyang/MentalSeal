package tournaments;

import MentalSeal.MentalSeal;
import MentalSeal.MentalSealPO;
import ai.abstraction.LightRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.core.AI;
import rts.units.UnitTypeTable;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class test {
    public static void main(String[] args) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();
        List<String> maps = new LinkedList<>();
        maps.add("maps/10x10/basesWorkers10x10.xml");
        List<AI> ais = new ArrayList<>();
        ais.add(new MentalSealPO(utt));
        ais.add(new POWorkerRush(utt));
        ais.add(new POLightRush(utt));
        RoundRobinTournament t = new RoundRobinTournament(ais);
        PrintWriter out = new PrintWriter(new File("results.txt"));
        t.runTournament(0, maps, 10, 3000, 100, -1, 10001,
                2000, false, false, false, true, true, utt, "traces", out, out, "RW");
    }
}