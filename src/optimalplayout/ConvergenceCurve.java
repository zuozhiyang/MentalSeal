//package optimalplayout;
//
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.util.*;
//
//
//
//public class ConvergenceCurve {
//    public static void main(String[] args) throws Exception {
//        List<String> maps = new ArrayList<>();
//        List<Integer> maxGameLength = new ArrayList<>();
//
//        maps.add("maps/8x8/basesWorkers8x8A.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/8x8/FourBasesWorkers8x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/NoWhereToRun9x8.xml");
//        maxGameLength.add(3000);
//        maps.add("maps/16x16/basesWorkers16x16A.xml");
//        maxGameLength.add(4000);
//        maps.add("maps/16x16/TwoBasesBarracks16x16.xml");
//        maxGameLength.add(4000);
//        String dir = "gameplay-results-multi";
//        String[] pathnames;
//
//        File f = new File(dir);
//
//        pathnames = f.list();
//        BufferedWriter writer = new BufferedWriter(new FileWriter("evaluation-results/evaluations-multi1.txt"));
//        BufferedWriter name = new BufferedWriter(new FileWriter("evaluation-results/name-multi1.txt"));
//        HashMap<String, double[]> lookup = new HashMap<>();
//        HashMap<String, Double> history = new HashMap<>();
//
//        for (String pathname : pathnames) {
//            if (!pathname.startsWith("console") && !pathname.startsWith("META") ) {
//                System.out.println(pathname);
//                File myObj = new File(dir + "/" + pathname);
//                Scanner myReader = new Scanner(myObj);
//                String[] s = pathname.split("-");
//
//                String map = s[4];
////                String key = s[0]+"-"+s[3];
////                System.out.println(key);
////                System.out.println(Arrays.toString(s));
//                String key = s[0]+s[1]+"-"+(s[4].length()>5?s[4]:s[3]);
////                String key = s[0]+"-"+(s[3].length()>5?s[3]:s[2]);
////                System.out.println(key);
//                int m = 0;
//                for (int i = 0; i < maps.size(); i++) {
//                    if (maps.get(i).contains(map)) {
//                        m = i;
//                        break;
//                    }
//                }
//                int j = 0;
//                double[] value = lookup.getOrDefault(key, new double[100]);
//                lookup.put(key,value);
//                while (myReader.hasNextLine()) {
//                    String[] data = myReader.nextLine().replaceAll("\\[|\\]| ", "").split(",");
//                    int[] values = new int[6];
//                    for (int i = 0; i < values.length; i++) {
//                        values[i] = Integer.parseInt(data[i+1]);
//                    }
//                    double evaluation = 0;
//                    if (history.containsKey(key+ Arrays.toString(values))) {
//                        evaluation = history.get(key+ Arrays.toString(values));
//                    } else {
//                        evaluation = parallelRun(20, 500, values, maps.get(m), maxGameLength.get(m));
//                        history.put(key+ Arrays.toString(values), evaluation);
//                    }
//                    value[j] += evaluation;
//                    j++;
//                }
//            }
//        }
//        for (String key : lookup.keySet()) {
//            name.write(key + "\n");
//            for (Double value : lookup.get(key)) {
//                writer.write((value / 20)+",");
//            }
//            writer.write("\n");
//        }
//        writer.flush();
//        name.flush();
//        writer.close();
//        name.close();
//    }
//
//}
