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
import java.util.HashMap;

public class AxonMain {
    public static void main(String[] args) {
        // write your code here
        org.apache.log4j.Logger logger = Logger.getLogger(AxonCall.class.getName());

        String loginURL = args[0];
        String username = args[1];
        String password = args[2];
        String postURL = args[3];
        String mainFacet = args[4];

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
        axonCall.queryAxon();
        logger.info(axonCall.getResultCode());
        logger.info(axonCall.getResultMessage());

        ArrayList<ArrayList<String>> axonData = axonCall.getAxonDataRecords();

        logger.info("axonData contains >" + axonData.size() + "< records.");

        for (ArrayList<String> record : axonData) {
            logger.info(record.toString());
        }

    }
}
