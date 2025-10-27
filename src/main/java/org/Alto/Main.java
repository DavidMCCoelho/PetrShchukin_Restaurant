package org.Alto;

import java.util.*;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) throws InterruptedException {

        if (args.length == 0) {
            new RestManager().run();
        }
        else {
            RestManager rm = new RestManager(args[0]);
            rm.run(processParams(args[1]));
        }

    }

    public static List<String> processParams(String param) {
        List<String> ret = new ArrayList<>();
        StringBuilder accu = new StringBuilder();
        int curlyBraces = 0;
        for (char c : param.toCharArray()) {
            if (c == '{')
                ++curlyBraces;
            else if (c == '}')
                --curlyBraces;
            else if (c == ',' && curlyBraces == 0) {
                ret.add(accu.toString().replace("{{", "{").replace("}}", "}"));
                accu = new StringBuilder();
                continue;
            }
            accu.append(c);
        }
        if (!accu.isEmpty())
            ret.add(accu.toString().replace("{{", "{").replace("}}", "}"));
        return ret;
    }
}

