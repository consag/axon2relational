## axon2relational
* Command line tool to convert a Informatica Axon JSON API results to a relational set
* Integrated into Informatica Developer 10.2.2, it can be used in a Java Transformation to prevent the impossibilities within the tool to handle complex JSONs.

## Examples
In the resources folder you will Informatica 10.2.2 exports of the Java transformation, a mapping and application that uses it.

## Code snippets
If you just want to use the transformation, it is unlikely you need to change the Java code itself. The code snippets however can be found in the folder src/main/resources/code_snippets. For each Tab of the axon2relational transformation you dragged/dropped in the Developer Client, you will find a corresponding file.

## Usage
### Input
- logLevel <br />
  Optional or Required: Required <br />
  Purpose: Determines the amount and details of log information. <br />
  Possible values (from detailed to only on Errors): DEBUG, INFO, WARN, ERROR <br />
- url <br />
  Optional or Required: Required <br />
  Purpose: The URL under which the Axon API can be reached<br />
  Possible values: Anything valid up to 1000 characters <br />
- mainfacet_value <br />
  Optional or Required: Required <br />
  Purpose: Axon facet to use as input for the API call.<br />
  Possible values: A valid Axon facet, eg. system<br />

### Output
- depends on the method used.
