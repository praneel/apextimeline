package com.anand.salesforce.log.operations;

import com.anand.salesforce.log.operations.Operation.EntryOrExit;

public class DMLOperation extends Operation {

	protected Integer rowCount;
	public DMLOperation(long execStartTime, String[] logLineTokens) {
		super(execStartTime,logLineTokens);
        if(this.eventSubType==EntryOrExit.BEGIN){
        	this.name = logLineTokens[4];
        	rowCount = Integer.parseInt(logLineTokens[5].substring(logLineTokens[5].indexOf(':')+1));
        }
	}
	public Integer getRowCount() {
		return rowCount;
	}
	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

}
