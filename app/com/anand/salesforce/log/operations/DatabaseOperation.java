package com.anand.salesforce.log.operations;


public class DatabaseOperation extends Operation {

	protected Integer rowCount;
	public DatabaseOperation(long execStartTime, String[] logLineTokens) {
		super(execStartTime,logLineTokens);
	}
	public Integer getRowCount() {
		return rowCount;
	}
	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

}
