package com.anand.salesforce.log.operations;

public class QueryOperation extends Operation {


	public QueryOperation(long execStartTime, String[] logLineTokens) {
		super(execStartTime,logLineTokens);
		if(this.eventSubType==EntryOrExit.BEGIN){
			this.name = logLineTokens[4];
		}
	}

}
