/*
 * MIT License
 *
 * Copyright (c) 2019 Jac. Beekers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package nl.jacbeekers;

import org.apache.log4j.Logger;
import java.util.ArrayList;

public class AxonMain {
    public static void usage(org.apache.log4j.Logger logger) {
        logger.info("Usage:");
        logger.info(AxonMain.class.getName() +" <loginURL> <username> <password> <queryURL> <mainFacet> [maxNrRecords]" );
        logger.info("where:");
        logger.info("  <loginURL> is the complete login URL for Axon, including http(s), hostname, port.");
        logger.info("  <username> is the Axon username to be used.");
        logger.info("  <password> is the password of the Axon username. In this version clear text (unfortunately).");
        logger.info("  <queryURL> is the complete URL for the Axon API queries.");
        logger.info("  <mainFacet> is the facet you want the data for, e.g. SYSTEM, DATASET or ATTRIBUTE. Case insensitive");
        logger.info("  [maxNrRecords] is optional and limits the result to the number mentioned. Default=10000");
    }

    public static void main(String[] args) {
        org.apache.log4j.Logger logger = Logger.getLogger(AxonMain.class.getName());

        if (args.length < 5) {
            usage(logger);
            return;
        }
        int maxInLog = 100;

        String loginURL = args[0];
        String username = args[1];
        String password = args[2];
        String postURL = args[3];
        String mainFacet = args[4];
        String maxDataRequested = "10000";
        if (args.length >5) {
            maxDataRequested = args[5];
            logger.info("Maximum number of records to be retrieved is >" + maxDataRequested + "<.");
        }

        AxonCall axonCall =null;

        axonCall = new AxonCall();

        axonCall.setLoginURL(loginURL);
        axonCall.login(username, password);
        logger.info(axonCall.getResultCode());
        logger.info(axonCall.getResultMessage());

        if (!Constants.OK.equals(axonCall.getResultCode())) {
            return;
        }

        axonCall.setQueryURL(postURL);
        axonCall.setMainFacet(mainFacet);
        axonCall.setLimit(maxDataRequested);
        axonCall.queryAxon();
        logger.info(axonCall.getResultCode());
        logger.info(axonCall.getResultMessage());

        ArrayList<ArrayList<String>> axonData = axonCall.getAxonDataRecords();
        ArrayList<String> axonFields = axonCall.getAxonDataFields();

        logger.info("axonData contains >" + axonData.size() + "< records.");

        logger.info("The fields : " + axonFields.toString());
        int i =0;
        for (ArrayList<String> record : axonData) {
            i++;
            if (i>maxInLog) {
                break;
            }

            logger.info("record is " + removeNonBMPCharacters(record.toString()));
        }

    }
    private static String removeNonBMPCharacters(final String input) {
        StringBuilder strBuilder = new StringBuilder();
        input.codePoints().forEach((i) -> {
            if (Character.isSupplementaryCodePoint(i)) {
                strBuilder.append("?");
            } else {
                strBuilder.append(Character.toChars(i));
            }
        });
        return strBuilder.toString();
    }
}
