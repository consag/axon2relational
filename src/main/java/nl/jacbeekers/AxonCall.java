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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import static java.net.HttpURLConnection.HTTP_OK;

public class AxonCall {
    private static final org.apache.log4j.Logger logger = Logger.getLogger(AxonCall.class.getName());

    private int nrRows;
    private String resultCode = Constants.OK;
    private String resultMessage = Constants.getResultMessage(resultCode);

    // query
    private String limit = "1000";

    // response
    private String response = Constants.UNKNOWN;
    private String facetId = Constants.UNKNOWN;
    private String totalItems = "-1";
    private String totalHits = "-1";
    private ArrayList<ArrayList<String>> axonDataRecords = new ArrayList<ArrayList<String>>();
    private ArrayList<String> axonDataFields = new ArrayList<String>();

    private ArrayList<AxonSystem> axonSystems = new ArrayList<AxonSystem>();
    private ArrayList<AxonDataset> axonDatasets = new ArrayList<AxonDataset>();
    private ArrayList<AxonAttribute> axonAttributes = new ArrayList<AxonAttribute>();

    // login
    private String username = Constants.NOT_PROVIDED;
    private String password = Constants.NOT_PROVIDED;
    private String token = Constants.NOT_PROVIDED;
    private String loginURL = Constants.NOT_PROVIDED;
    private String queryURL = Constants.NOT_PROVIDED;
    private String mainFacet = Constants.NOT_PROVIDED;

    public String login(String username, String password) {
        setUsername(username);
        setPassword(password);
        setToken(loginRequest());

        return getResultCode();
    }


    /*
     loginRequest
     */
    public String loginRequest() {
        String procName = "loginRequest";
        String jsonString = "";
        String keyValue = "";

        try {
            ArrayList<headerStructure> headerStrings = new ArrayList<headerStructure>();
            ArrayList<JSONStructure> postStrings = new ArrayList<JSONStructure>();
            JSONStructure username = new JSONStructure();
            username.setKey("username");
            username.setValue(getUsername());
            postStrings.add(username);
            JSONStructure password = new JSONStructure();
            password.setKey("password");
            password.setValue(getPassword());
            postStrings.add(password);

            POSTRequest(getLoginURL(), postStrings);
            jsonString = getResponse();
            if (jsonString == null) {
                logError(Constants.LOGIN_FAILED, Constants.getResultMessage(Constants.LOGIN_FAILED) + " - token is null");
            } else {
                LoginResponse response = new Gson().fromJson(jsonString, LoginResponse.class);
                keyValue = response.token;
            }
        } catch (IOException e) {
            logError(Constants.LOGIN_FAILED, Constants.getResultMessage(Constants.LOGIN_FAILED) + " - " + e.getMessage());
            return Constants.LOGIN_FAILED;
        }
//        logDebug(procName, "The token is >" + keyValue + "<.");
        return keyValue;
    }

    /*
     * queryAxon
     */

    public void queryAxon() {
        String procName = "queryAxon";
        String jsonString = "";

/*        ArrayList<JSONStructure> postStrings = new ArrayList<JSONStructure>();
        JSONStructure mainfacet = new JSONStructure();
        mainfacet.setKey("mainFacet");
        mainfacet.setValue(getMainFacet());
*/
        SystemQuery systemQuery = new SystemQuery();
        systemQuery.setMainFacet(getMainFacet());
        systemQuery.setSearchScope(getMainFacet(), "0", getLimit(), "0");
        String requestBody = new Gson().toJson(systemQuery);
        logDebug(procName, "Gson generated requestBody: " + requestBody);

//        postStrings.add(mainfacet);

        try {
            POSTRequest(getQueryURL(), requestBody);
            jsonString = getResponse();
            if (jsonString == null) {
                logError(Constants.QUERY_FAILED, Constants.getResultMessage(Constants.QUERY_FAILED) + " - response is null");
            } else {
                logDebug(procName, jsonString);
                jsonString = jsonString.substring(1, jsonString.lastIndexOf(']'));
                logDebug(procName, "without first and last char: " + jsonString);
                switch (getMainFacet()) {
                    case "system":
                        SystemResponse systemResponse = new Gson().fromJson(jsonString, SystemResponse.class);
                        logDebug(procName, "Total items found: " + systemResponse.totalHits);
                        setFacetId(systemResponse.facetId);
                        setTotalItems(systemResponse.totalItems);
                        setTotalHits(systemResponse.totalHits);
                        generateSystemDataset(systemResponse);
                        break;
                    case "dataset":
                        DatasetResponse datasetResponse = new Gson().fromJson(jsonString, DatasetResponse.class);
                        logDebug(procName, "Total items found: " + datasetResponse.totalHits);
                        setFacetId(datasetResponse.facetId);
                        setTotalItems(datasetResponse.totalItems);
                        setTotalHits(datasetResponse.totalHits);
                        generateDatasetDataset(datasetResponse);
                        break;
                    case "attribute":
                        AttributeResponse attributeResponse = new Gson().fromJson(jsonString, AttributeResponse.class);
                        logDebug(procName, "Total items found: " + attributeResponse.totalHits);
                        setFacetId(attributeResponse.facetId);
                        setTotalItems(attributeResponse.totalItems);
                        setTotalHits(attributeResponse.totalHits);
                        generateAttributeDataset(attributeResponse);
                        break;
                    default:
                        logError(procName, "facet >" + getMainFacet() + "< not yet supported.");
                        break;
                }
            }
        } catch (IOException e) {
            logError(Constants.QUERY_FAILED, Constants.getResultMessage(Constants.QUERY_FAILED) + " - " + e.getMessage());
        }

    }

    public String POSTBody(ArrayList<JSONStructure> keyvalueList) {
        String bodyString = "{";
        boolean first = true;

        for (JSONStructure item : keyvalueList) {
            if (first) {
                first = false;
            } else {
                bodyString += ",";
            }
            bodyString += item.getKey() + ":" + item.getValue();
        }
        bodyString += "}";

        return bodyString;
    }

    public String POSTRequest(String URL, String jsonBody) throws IOException {
        String procName = "POSTRequest";

        URL obj = new URL(URL);
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();

        postConnection.setDoInput(true);
        postConnection.setDoOutput(true);
        postConnection.setRequestMethod("POST");


        postConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        postConnection.setRequestProperty("Authorization", "Bearer " + getToken());

        OutputStream os = postConnection.getOutputStream();
        os.write(jsonBody.getBytes());
        logDebug(procName, "Written POST Body >" + jsonBody + "<.");
        os.flush();
        os.close();
        int responseCode = postConnection.getResponseCode();
        logDebug(procName, "POST Response Code :  " + responseCode);
        logDebug(procName, "POST Response Message : " + postConnection.getResponseMessage());
        if (responseCode == HTTP_OK) { //success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    postConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            setResponse(response.toString());
        } else {
            setResult(Constants.POST_FAILED, Constants.getResultMessage(Constants.POST_FAILED) + " - " + postConnection.getResponseMessage());
            setResponse(null);
        }

        return getResponse();

    }

    public String POSTRequest(String URL, ArrayList<JSONStructure> postStrings) throws IOException {
        String procName = "POSTRequest";
        String POSTString = "";
        POSTString = POSTBody(postStrings);

        return POSTRequest(URL, POSTString);

    }

    private void generateSystemDataset(SystemResponse systemResponse) {
        String procName = "generateSystemDataset";
        ArrayList<ArrayList<String>> axonData = new ArrayList<ArrayList<String>>();
        ArrayList<AxonSystem> axonSystems = new ArrayList<AxonSystem>();

        for (SystemItem systemItem : systemResponse.items) {
            axonData.add(systemItem.values);
            AxonSystem axonSystem = new AxonSystem();
            logVerbose("record values: " + systemItem.values);
            for (int fieldnr = 0; fieldnr < systemResponse.fields.size(); fieldnr++) {
                logVerbose("fieldnr >" + fieldnr + "< named >" + systemResponse.fields.get(fieldnr) + "< has value >"
                        + systemItem.values.get(fieldnr) + "<.");
                // cannot use gson as we are not sure that the field list (and therefore the value list) will always be in the same order as the attributes in the class
                // if you are an experienced Java developer: let us know how to achieve this better.
                switch (systemResponse.fields.get(fieldnr)) {
                    case "id":
                        axonSystem.id = systemItem.values.get(fieldnr);
                        break;
                    case "name":
                        axonSystem.name = systemItem.values.get(fieldnr);
                        break;
                    case "description":
                        axonSystem.description = systemItem.values.get(fieldnr);
                        break;
                    case "parentId":
                        axonSystem.parentId = systemItem.values.get(fieldnr);
                        break;
                    case "parentName":
                        axonSystem.parentName = systemItem.values.get(fieldnr);
                        break;
                    case "type":
                        axonSystem.type = systemItem.values.get(fieldnr);
                        break;
                    case "axonStatus":
                        axonSystem.axonStatus = systemItem.values.get(fieldnr);
                        break;
                    case "longName":
                        axonSystem.longName = systemItem.values.get(fieldnr);
                        break;
                    case "lifecycle":
                        axonSystem.lifecycle = systemItem.values.get(fieldnr);
                        break;
                    case "classification":
                        axonSystem.classification = systemItem.values.get(fieldnr);
                        break;
                    case "createdDate":
                        axonSystem.createdDate = systemItem.values.get(fieldnr);
                        break;
                    case "lastUpdatedDate":
                        axonSystem.lastUpdatedDate = systemItem.values.get(fieldnr);
                        break;
                    case "accessControl":
                        axonSystem.accessControl = systemItem.values.get(fieldnr);
                        break;
                    case "crating":
                        axonSystem.crating = systemItem.values.get(fieldnr);
                        break;
                    case "irating":
                        axonSystem.irating = systemItem.values.get(fieldnr);
                        break;
                    case "arating":
                        axonSystem.arating = systemItem.values.get(fieldnr);
                        break;
                    case "ciarating":
                        axonSystem.ciarating = systemItem.values.get(fieldnr);
                        break;
                    default:
                        logError(Constants.DATA_STRUCTURE_ERROR, "Fieldname in JSON is not part of Java class structure. Please report this error to the developer.");
                        break;
                }
            }
            axonSystems.add(axonSystem);
        }

        setAxonDataFields(systemResponse.fields);
        setAxonDataRecords(axonData);
        setAxonSystems(axonSystems);

    }

    private void generateDatasetDataset(DatasetResponse datasetResponse) {
        ArrayList<ArrayList<String>> axonData = new ArrayList<ArrayList<String>>();
        ArrayList<AxonDataset> axonDatasets = new ArrayList<AxonDataset>();

        for (DatasetItem datasetItem : datasetResponse.items) {
            axonData.add(datasetItem.values);
        }

        for (DatasetItem datasetItem : datasetResponse.items) {
            axonData.add(datasetItem.values);
            AxonDataset axonDataset = new AxonDataset();
            logVerbose("record values: " + datasetItem.values);
            for (int fieldnr = 0; fieldnr < datasetResponse.fields.size(); fieldnr++) {
                logVerbose("fieldnr >" + fieldnr + "< named >" + datasetResponse.fields.get(fieldnr) + "< has value >"
                        + datasetItem.values.get(fieldnr) + "<.");
                // cannot use gson as we are not sure that the field list (and therefore the value list) will always be in the same order as the attributes in the class
                // if you are an experienced Java developer: let us know how to achieve this better.
                switch (datasetResponse.fields.get(fieldnr)) {
                    case "createdDate":
                        axonDataset.createdDate = datasetItem.values.get(fieldnr);
                        break;
                    case "lastUpdatedDate":
                        axonDataset.lastUpdatedDate = datasetItem.values.get(fieldnr);
                        break;
                    case "id":
                        axonDataset.id = datasetItem.values.get(fieldnr);
                        break;
                    case "name":
                        axonDataset.name = datasetItem.values.get(fieldnr);
                        break;
                    case "definition":
                        axonDataset.definition = datasetItem.values.get(fieldnr);
                        break;
                    case "refNumber":
                        axonDataset.refNumber = datasetItem.values.get(fieldnr);
                        break;
                    case "type":
                        axonDataset.type = datasetItem.values.get(fieldnr);
                        break;
                    case "axonStatus":
                        axonDataset.axonStatus = datasetItem.values.get(fieldnr);
                        break;
                    case "lifecycle":
                        axonDataset.lifecycle = datasetItem.values.get(fieldnr);
                        break;
                    case "systemId":
                        axonDataset.systemId = datasetItem.values.get(fieldnr);
                        break;
                    case "systemName":
                        axonDataset.systemName = datasetItem.values.get(fieldnr);
                        break;
                    case "glossaryId":
                        axonDataset.glossaryId = datasetItem.values.get(fieldnr);
                        break;
                    case "glossaryName":
                        axonDataset.glossaryName = datasetItem.values.get(fieldnr);
                        break;
                    case "accessControl":
                        axonDataset.accessControl = datasetItem.values.get(fieldnr);
                        break;
                    default:
                        logError(Constants.DATA_STRUCTURE_ERROR, "Fieldname in JSON is not part of Java class structure. Please report this error to the developer.");
                        break;

                }
            }
            axonDatasets.add(axonDataset);
        }

        setAxonDataFields(datasetResponse.fields);
        setAxonDataRecords(axonData);
        setAxonDatasets(axonDatasets);
    }

    private void generateAttributeDataset(AttributeResponse attributeResponse) {
        ArrayList<ArrayList<String>> axonData = new ArrayList<ArrayList<String>>();

        for (AttributeItem attributeItem : attributeResponse.items) {
            axonData.add(attributeItem.values);
        }

        ArrayList<AxonAttribute> axonAttributes = new ArrayList<AxonAttribute>();

        for (AttributeItem attributeItem : attributeResponse.items) {
            axonData.add(attributeItem.values);
        }

        for (AttributeItem attributeItem : attributeResponse.items) {
            axonData.add(attributeItem.values);
            AxonAttribute axonAttribute = new AxonAttribute();
            logVerbose("record values: " + attributeItem.values);
            for (int fieldnr = 0; fieldnr < attributeResponse.fields.size(); fieldnr++) {
                logVerbose("fieldnr >" + fieldnr + "< named >" + attributeResponse.fields.get(fieldnr) + "< has value >"
                        + attributeItem.values.get(fieldnr) + "<.");
                // cannot use gson as we are not sure that the field list (and therefore the value list) will always be in the same order as the attributes in the class
                // if you are an experienced Java developer: let us know how to achieve this better.
                switch (attributeResponse.fields.get(fieldnr)) {
                    case "createdDate":
                        axonAttribute.createdDate = attributeItem.values.get(fieldnr);
                        break;
                    case "lastUpdatedDate":
                        axonAttribute.lastUpdatedDate = attributeItem.values.get(fieldnr);
                        break;
                    case "id":
                        axonAttribute.id = attributeItem.values.get(fieldnr);
                        break;
                    case "name":
                        axonAttribute.name = attributeItem.values.get(fieldnr);
                        break;
                    case "definition":
                        axonAttribute.definition = attributeItem.values.get(fieldnr);
                        break;
                    case "refNumber":
                        axonAttribute.refNumber = attributeItem.values.get(fieldnr);
                        break;
                    case "dataSetId":
                        axonAttribute.dataSetId = attributeItem.values.get(fieldnr);
                        break;
                    case "dataSetName":
                        axonAttribute.dataSetName = attributeItem.values.get(fieldnr);
                        break;
                    case "dbName":
                        axonAttribute.dbName = attributeItem.values.get(fieldnr);
                        break;
                    case "systemId":
                        axonAttribute.systemId = attributeItem.values.get(fieldnr);
                        break;
                    case "systemName":
                        axonAttribute.systemName = attributeItem.values.get(fieldnr);
                        break;
                    case "origination":
                        axonAttribute.origination = attributeItem.values.get(fieldnr);
                        break;
                    case "discoveryReviewStatus":
                        axonAttribute.discoveryReviewStatus = attributeItem.values.get(fieldnr);
                        break;
                    case "confidenceScore":
                        axonAttribute.confidenceScore = attributeItem.values.get(fieldnr);
                        break;
                    case "editabilityRole":
                        axonAttribute.editabilityRole = attributeItem.values.get(fieldnr);
                        break;
                    case "editability":
                        axonAttribute.editability = attributeItem.values.get(fieldnr);
                        break;
                    case "glossaryId":
                        axonAttribute.glossaryId = attributeItem.values.get(fieldnr);
                        break;
                    case "glossaryName":
                        axonAttribute.glossaryName = attributeItem.values.get(fieldnr);
                        break;
                    case "mandatory":
                        axonAttribute.mandatory = attributeItem.values.get(fieldnr);
                        break;
                    default:
                        logError(Constants.DATA_STRUCTURE_ERROR, "Fieldname in JSON is not part of Java class structure. Please report this error to the developer.");
                        break;

                }
            }
            axonAttributes.add(axonAttribute);
        }

        setAxonDataFields(attributeResponse.fields);
        setAxonDataRecords(axonData);
        setAxonAttributes(axonAttributes);
    }

    //
    //getters setters
    //


    public ArrayList<AxonSystem> getAxonSystems() {
        return axonSystems;
    }

    public void setAxonSystems(ArrayList<AxonSystem> axonSystems) {
        this.axonSystems = axonSystems;
    }

    public ArrayList<AxonDataset> getAxonDatasets() {
        return axonDatasets;
    }

    public void setAxonDatasets(ArrayList<AxonDataset> axonDatasets) {
        this.axonDatasets = axonDatasets;
    }

    public ArrayList<AxonAttribute> getAxonAttributes() {
        return axonAttributes;
    }

    public void setAxonAttributes(ArrayList<AxonAttribute> axonAttributes) {
        this.axonAttributes = axonAttributes;
    }

    public ArrayList<String> getAxonDataFields() {
        return axonDataFields;
    }

    public void setAxonDataFields(ArrayList<String> axonDataFields) {
        this.axonDataFields = axonDataFields;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    private void setAxonDataRecords(ArrayList<ArrayList<String>> axonDataRecords) {
        this.axonDataRecords = axonDataRecords;
    }

    public ArrayList<ArrayList<String>> getAxonDataRecords() {
        return this.axonDataRecords;
    }

    private String getUsername() {
        return this.username;
    }

    public String getFacetId() {
        return this.facetId;
    }

    public void setFacetId(String facetId) {
        this.facetId = facetId;
    }

    public String getTotalItems() {
        return this.totalItems;
    }

    public int getTotalItemsAsInt() {
        return Integer.getInteger(getTotalItems());
    }

    private void setTotalItems(String totalItems) {
        this.totalItems = totalItems;
    }

    public String getTotalHits() {
        return totalHits;
    }

    public int getTotalHitsAsInt() {
        return Integer.getInteger(getTotalHits());
    }

    private void setTotalHits(String totalHits) {
        this.totalHits = totalHits;
    }

    public void setQueryURL(String queryURL) {
        this.queryURL = queryURL;
    }

    public String getQueryURL() {
        return this.queryURL;
    }

    public void setMainFacet(String mainFacet) {
        this.mainFacet = mainFacet;
    }

    public String getMainFacet() {
        return this.mainFacet;
    }

    private void setNrRows(int nrRows) {
        this.nrRows = nrRows;
    }

    public int getNrRows() {
        return nrRows;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginURL() {
        return this.loginURL;
    }

    public void setLoginURL(String URL) {
        this.loginURL = URL;
    }

    private String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResponse() {
        return this.response;
    }

    private void setResponse(String response) {
        this.response = response;
    }

    public String getFormattedCurrentTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = sdf.format(date);

        return formattedTime;
    }

    public String getResultCode() {
        return this.resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return this.resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    //
    // logging, result handling
    //
    private void logVerbose(String msg) {
        logger.trace(msg);
    }

    private void logDebug(String procName, String msg) {
        logger.debug(procName + " - " + msg);
    }

    private void logDebug(String msg) {
        logger.debug(msg);
    }

    private void logWarning(String msg) {
        logger.warn(msg);
    }

    private void logError(String resultCode, String msg) {
        setResult(resultCode, msg);
        logger.error(msg);
    }

    private void setResult(String resultCode, String msg) {
        setResultCode(resultCode);
        if (msg == null) {
            setResultMessage(Constants.getResultMessage(resultCode));
        } else {
            setResultMessage(Constants.getResultMessage(resultCode)
                    + ": " + msg);
        }
    }

    private void logFatal(String resultCode) {
        logFatal(resultCode, Constants.getResultMessage(resultCode));
    }

    private void logFatal(String resultCode, String msg) {
        setResult(resultCode, msg);
        logger.fatal(msg);
    }

    private void failSession(String resultCode) {
        failSession(resultCode, null);
    }

    private void failSession(String resultCode, String msg) {
        logError(resultCode, msg);
    }


}

class headerStructure {
    String key;
    String value;

    void setKey(String key) {
        this.key = key;
    }

    void setValue(String value) {
        this.value = value;
    }
}

class JSONStructure {
    String key;
    String value;

    public String quotinize(String str) {
        return "\"" + str + "\"";
    }

    void setKey(String key) {
        this.key = quotinize(key);
    }

    String getKey() {
        return key;
    }

    void setValue(String value) {
        this.value = quotinize(value);
    }

    String getValue() {
        return value;
    }
}

class SystemQuery {
    String mainFacet;
    ArrayList<SearchScope> searchScopes = new ArrayList<SearchScope>();

    void setSearchScope(String scope, String offset, String limit, String childrenLevel) {
        SearchScope searchScope = new SearchScope();
        searchScope.setFacetId(scope);
        searchScope.setLimits(offset, limit, childrenLevel);
        searchScopes.add(searchScope);
    }

    ArrayList<SearchScope> getSearchScopes() {
        return this.searchScopes;
    }

    void setMainFacet(String mainFacet) {
        this.mainFacet = mainFacet;
    }

    String getMainFacet() {
        return this.mainFacet;
    }
}

class SearchProperties {
    String offset;
    String limit;
    String levelChildren;

    void setOffset(String offset) {
        this.offset = offset;
    }

    void setLimit(String limit) {
        this.limit = limit;
    }

    void setLevelChildren(String levelChildren) {
        this.levelChildren = levelChildren;
    }
}

class SearchScope {
    String facetId;
    SearchProperties properties = new SearchProperties();

    void setFacetId(String facetId) {
        this.facetId = facetId;
    }

    void setLimits(String offset, String limit, String childrenLevel) {
        properties.setOffset(offset);
        properties.setLimit(limit);
        properties.setLevelChildren(childrenLevel);
    }
}

class LoginResponse {
    String token;
}

class SystemResponse {
    String facetId;
    String totalItems;
    String totalHits;
    ArrayList<String> fields;
    ArrayList<SystemItem> items;
}

class SystemItem {
    String ref;
    String id;
    ArrayList<String> values;
}

class AxonSystem {
    String id;
    String name;
    String description;
    String parentId;
    String parentName;
    String type;
    String axonStatus;
    String longName;
    String lifecycle;
    String classification;
    String createdDate;
    String lastUpdatedDate;
    String accessControl;
    String crating;
    String irating;
    String arating;
    String ciarating;
}

class DatasetResponse {
    String facetId;
    String totalItems;
    String totalHits;
    ArrayList<String> fields;
    ArrayList<DatasetItem> items;
}

class DatasetItem {
    String ref;
    String id;
    ArrayList<String> values;
}

class AxonDataset {
    String id;
    String name;
    String definition;
    String refNumber;
    String type;
    String axonStatus;
    String lifecycle;
    String systemId;
    String systemName;
    String glossaryId;
    String glossaryName;
    String createdDate;
    String lastUpdatedDate;
    String accessControl;
}

class AttributeResponse {
    String facetId;
    String totalItems;
    String totalHits;
    ArrayList<String> fields;
    ArrayList<AttributeItem> items;
}

class AttributeItem {
    String ref;
    String id;
    ArrayList<String> values;
}

class AxonAttribute {
    String id;
    String name;
    String definition;
    String refNumber;
    String dataSetId;
    String dataSetName;
    String dbName;
    String systemId;
    String systemName;
    String origination;
    String discoveryReviewStatus;
    String confidenceScore;
    String editabilityRole;
    String editability;
    String glossaryId;
    String glossaryName;
    String mandatory;
    String createdDate;
    String lastUpdatedDate;
}