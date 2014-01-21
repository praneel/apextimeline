package com.anand.salesforce.log.utils;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;

import org.codehaus.jackson.map.ObjectMapper;

import com.anand.salesforce.log.operations.Operation;
import com.anand.salesforce.log.operations.DatabaseOperation;
import com.anand.salesforce.log.operations.Operation.EntryOrExit;
public class SFDCLogParser {
	public static final SimpleDateFormat DATE_FORMAT= new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
	public static void main(String args[]) throws Exception{
		SFDCLogParser parser = new SFDCLogParser();
		parser.parseLogFile(args[0],Integer.parseInt(args[1]));
	}
	
	public void parseLogFile(String fileName,Integer timeThreshold) throws Exception{
		File logFile = new File(fileName);
		parseLogFile(logFile,timeThreshold);
	}
	public Operation parseLogFile(File logFile,Integer timeThreshold) throws Exception{
		Stack<Operation> oprStack1 = new Stack<Operation>();
		BufferedReader reader = new BufferedReader(new FileReader(logFile));
		Operation currOp,prevOp,parentOpr;
		String currLine;
		currLine = reader.readLine();
		long execStartTime=-1;
		try{
			while((currLine = reader.readLine())!=null){
				if(currLine.indexOf('|')>=0){
					currOp = OperationFactory.createOperationForLogLine(execStartTime,currLine);
					if(currOp!=null){
						if(currOp.getEventType().equalsIgnoreCase("EXECUTION") && currOp.getEventSubType()==Operation.EntryOrExit.STARTED){
							execStartTime=currOp.getStartTime();
						}
						prevOp = oprStack1.isEmpty()?null:oprStack1.pop();
						if(prevOp!=null && prevOp.getEventId().equalsIgnoreCase(currOp.getEventId())){
							
							prevOp.setEndTime(currOp.getStartTime());
							
							//Set the row count for SOQL queries
							if(	currOp.getEventType().equalsIgnoreCase("SOQL") && currOp.getEventSubType() == EntryOrExit.END){
								((DatabaseOperation)prevOp).setRowCount(((DatabaseOperation)currOp).getRowCount());
							}
							if(	prevOp.getElapsedMillis()>=timeThreshold ||
								prevOp.getEventType().equalsIgnoreCase("SOQL") ||
								prevOp.getEventType().equalsIgnoreCase("DML")){
								//Pop the parent & add it to paren't long running operations
								parentOpr = oprStack1.isEmpty()?null:oprStack1.pop();
								if(parentOpr !=null){
									parentOpr.add(prevOp);
									//Add the parent back to the stack
									oprStack1.push(parentOpr);
								}else if(oprStack1.isEmpty()){
									//In the top most method, make sure you push the 
									//previous operation back into the stack
									oprStack1.push(prevOp);
								}
							}
							
						}else{
							if(prevOp!=null){
								oprStack1.push(prevOp);
							}
							oprStack1.push(currOp);
						}
					}
				}
			}
			
			return oprStack1.pop();
		}catch(Exception ex){
			return null;
		}finally{
			try{
				reader.close();
			}catch(Exception ex){
				
			}
		}
	}
	

	public List<Operation> getFlattenedDataForUI(Operation operation){
		List<Operation> oprList = new ArrayList<Operation>();
		oprList.add(operation);
		if(operation.hasOperations()){
			for(Operation opr : operation.getOperations()){
				if(opr.hasOperations()){
					oprList.addAll(getFlattenedDataForUI(opr));
				}else{
					oprList.add(opr);
				}
			}
		}
		return oprList;
	}

	public List<DatabaseOperation> getDatabaseOperations(Operation operation){
		List<DatabaseOperation> oprList = new ArrayList<DatabaseOperation>();
		if(operation.hasOperations()){
			for(Operation opr : operation.getOperations()){
				if(opr.hasOperations()){
					oprList.addAll(getDatabaseOperations(opr));
				}else{
					if(	opr.getEventType().equalsIgnoreCase("SOQL") || 
						opr.getEventType().equalsIgnoreCase("DML")	){
						oprList.add((DatabaseOperation)opr);
					}
				}
			}
		}
		return oprList;
	}

}
