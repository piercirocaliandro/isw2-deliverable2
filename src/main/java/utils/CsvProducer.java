package main.java.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import main.java.javabeans.JavaFile;
import main.java.javabeans.Ticket;
import main.java.javabeans.Version;

/* Create .csv file with the metrics for each class*/

public class CsvProducer {
	private List<Ticket> jiraTicketList;
	private List<JavaFile> projFiles;
	private List<String> mapKeys;
	
	private Map<String,Integer> sizeList; //size of the class, calculated in LOC
	private Map<String, Long> ageList; // ages, calculated in weeks
	private Map<String, Integer> nRev; // revisions 
	private Map<String, Integer> nAuthList; // authors for each class
	private Map<String, Integer> maxLocAddList; // sum over revision of LOC i + d + m
	private Map<String, Integer> locAddedList; // sum over revision of LOC added
	private Map<String, Integer> churnList; // sum over revision of LOC i - d
	private Map<String, String> buggynessMap;
	
	// parameters useful to apply proportion
	private float proportion = 0.0f;
	private int nBuggy = 0;
	private int nProp = 0;
	private int noAvs = 0;
	private Map<String, LocalDate> versions;
	
	private String project;
	private StringBuilder sb;
	
	private String constName = " metrics.csv";
	
	public CsvProducer(String projectName) {
		this.jiraTicketList = new ArrayList<>();
		this.projFiles = new ArrayList<>();
		this.versions = new HashMap<>();
		
		this.mapKeys = new ArrayList<>();
		
		this.sizeList = new LinkedHashMap<>();
		this.ageList = new LinkedHashMap<>();
		this.nRev = new LinkedHashMap<>();
		this.nAuthList = new LinkedHashMap<>();
		this.maxLocAddList = new LinkedHashMap<>();
		this.locAddedList = new LinkedHashMap<>();
		this.churnList = new LinkedHashMap<>();
		this.buggynessMap = new LinkedHashMap<>();
		
		this.project = projectName;
		this.deleteIfExists();
		
		this.sb = new StringBuilder();
	}
	
	
	public void setTickList(List<Ticket> tickList) {
		this.jiraTicketList = tickList;
	}
	
	public void setJavaFileList(List<JavaFile> jFileList) {
		this.projFiles = jFileList;
	}
	
	public void setVersions(Map<String, LocalDate> vers) {
		this.versions = vers;
	}
	
	public Integer getNBuggy() {
		return this.nBuggy;
	}
	
	public Integer getNoAv() {
		return this.noAvs;
	}
	
	public Integer getNProp() {
		return this.nProp;
	}
	
	public void setKeys(List<String> keys){
		this.mapKeys= keys;
	}
	
	public void initSize() {
		for(String key : this.mapKeys) {
			this.sizeList.put(key, 0);
		}
	}
	
	
	/* Insert the data for a specific version in the .csv file
	 * 
	 * @param release: the version id
	 * @param relDate: the release date of the version*/
	private void fillCsvWithMetrics(String release) {
		String cons =  this.project + constName;
		var f = new File(cons);
		
		try (var fw = new FileWriter(f.getAbsoluteFile(), true);
				var bw = new BufferedWriter(fw)){
			
			for(String key : this.mapKeys) {
				if(this.sizeList.get(key) > 0 && this.nRev.get(key) > 0) {
					sb.append(this.project + "," + release +"," + 
							key + "," + this.sizeList.get(key) +
							"," + this.nRev.get(key) + "," +this.nAuthList.get(key)+","+
							this.ageList.get(key) + ","+
							this.maxLocAddList.get(key) + ","+
							this.locAddedList.get(key) + "," + 
							this.locAddedList.get(key)/this.nRev.get(key) + "," + 
							this.churnList.get(key)+
							"," + this.churnList.get(key)/this.nRev.get(key)+
							"," + this.buggynessMap.get(key));
					bw.newLine();
					bw.write(sb.toString());
					sb.delete(0, sb.toString().length());
					if(this.buggynessMap.get(key).equals("yes"))
						this.nBuggy++;
				}
				else if(this.sizeList.get(key) > 0 && this.nRev.get(key) == 0) {
					sb.append(this.project+ "," + release +"," + 
							key + "," + this.sizeList.get(key) +
							"," + 0 + "," +0+","+
							this.ageList.get(key) + ","+
							0 + ","+
							0 + "," + 
							0 + "," + 
							0+
							"," + 0 + "," + this.buggynessMap.get(key));
					bw.newLine();
					bw.write(sb.toString());
					sb.delete(0, sb.toString().length());
					if(this.buggynessMap.get(key).equals("yes"))
						this.nBuggy++;
				}
			}
		}catch (FileNotFoundException e) {
				Logger.getLogger("LAB").log(Level.WARNING, "Cannot find the file\n");
		} catch (IOException e) {
				Logger.getLogger("LAB").log(Level.WARNING, e.getCause().getMessage(), e.getMessage());
		}
	}
	
	
	/* Write the header in the .csv file*/
	public void setCsvHeader() {
		String cons =  this.project+constName;
		var f = new File(cons);
		
		try (var fw = new FileWriter(f.getAbsoluteFile(), true);
				var bw = new BufferedWriter(fw)){
				
				sb.append("Project name"+","+"Release"+ "," + "Class name"
				+ "," + "Size" + "," + "NR" + "," + "NAuth" + "," + "Age"
				+","+"MAX_LOC_ADDED"+","+"LOC_ADDED"+","+"AVG_LOC_ADDDED"	+","+
				"Churn"+","+"AVG_CHURN"
				+ "," +"buggyness");
				bw.write(sb.toString());
				sb.delete(0, sb.toString().length());
				
		}catch (FileNotFoundException e) {
				Logger.getLogger("LAB").log(Level.WARNING, "Cannot find the file\n");
		} catch (IOException e) {
				Logger.getLogger("LAB").log(Level.WARNING, e.getCause().getMessage(), e.getMessage());
		}
	}
	
	
	/* Compute all the metrics for each class and for the specific version
	 * and report them in a .csv file 
	 * 
	 * @param startDate: the release date of the previous version
	 * @param endDate: the release date of the current version
	 * @param release: the current version */
	public void getAllMetrics(LocalDate startDate, LocalDate endDate, String release) 
			throws IOException {
		
		sb.append("Calculating metrics for release " + release);
		var temp = sb.toString();
		Logger.getLogger("CSV_PROD").info(temp);
		sb.delete(0, sb.length());
		
		List<RevCommit> commitList;
		var logAnalyzer = new LogAnalyzer(this.project.toLowerCase());
		List<Integer> tempMetrics;
		String currClass;
		
		// iterate over all the .java files
		for(JavaFile javaClass : projFiles) {
			commitList = javaClass.getCommitList();
			currClass = javaClass.getName();
			if(this.sizeList.get(currClass) <= 0) {
					tempMetrics = logAnalyzer.countLocForRelease(commitList, javaClass.getClassName(),
							startDate, endDate, true); // have to count the first commit
			}
			else
				tempMetrics = logAnalyzer.countLocForRelease(commitList, javaClass.getClassName(), 
						startDate,
						endDate, false);
			if(tempMetrics.get(3) > 0) { // at least one commit was processed
				incrMetrics(tempMetrics, currClass);
				this.nAuthList.put(currClass,
						setAuthorMetric(commitList, startDate, endDate));
			}
			setClassAge(javaClass, endDate); // the age has to be incremented anyway
		}
		
		setBuggynessWithAv(release);
		fillCsvWithMetrics(release);
		resetVersMetrics();
	}
	
	
	/* Increase all the integer-valued metrics for all the classes
	 * 
	 * @param metrics: the lines of code (added - removed) to sum to the size
	 * @param className: name of the .java class under analysis */
	private void incrMetrics(List<Integer> metrics, String className) {
		if(this.sizeList.get(className) <= 0) {
			this.churnList.put(className, metrics.get(0)-metrics.get(1));
			this.locAddedList.put(className, metrics.get(0));
			this.maxLocAddList.put(className, metrics.get(2));
			this.nRev.put(className, metrics.get(3));
		}
		else {
			this.churnList.replace(className, metrics.get(0)-metrics.get(1));
			this.locAddedList.replace(className, metrics.get(0));
			this.maxLocAddList.replace(className, metrics.get(2));
			this.nRev.replace(className, metrics.get(3));
		}
		this.sizeList.replace(className, this.sizeList.get(className)+ 
				metrics.get(0)-metrics.get(1));
	}
	
	
	/* Set the metric of the number of authors that have made at least one commit 
	 * for a specific .java class in the current release
	 * 
	 * @param commitList: list of the commit for that class
	 * @param startDate: release date of the previous version (lower bound) 
	 * @param endDate: release date of the current version (upper bound) */
	private int setAuthorMetric(List<RevCommit> commitList, LocalDate startDate,
			LocalDate endDate) {
		var nAuth = 0;
		List<String> authors = new ArrayList<>();
		LocalDate commDate;
		
		for(RevCommit commit : commitList) {
		PersonIdent curr = commit.getAuthorIdent();
		commDate = curr.getWhen().toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
		
		if(startDate == null) {
			if(commDate.isBefore(endDate) &&
					!authors.contains(curr.getName()))
				nAuth += 1;
			authors.add(curr.getName());
		}
		else {
			if((commDate.isAfter(startDate) || commDate.isEqual(startDate))
				&& commDate.isBefore(endDate) &&
				!authors.contains(curr.getName())) {
				
					nAuth += 1;
					authors.add(curr.getName());
				}
			}
		}
		return nAuth;
	}
	
	
	/* Compute the age, in weeks, for the class
	 * 
	 *  @param projFile: the instance of JavaFile  
	 *  @param relDate: release date of the current version */
	private void setClassAge(JavaFile projFile, 
			LocalDate relDate) {
		
		// the class has not been created yet
		if(this.sizeList.get(projFile.getName()) <= 0)
				return;
		
		var weeks = ChronoUnit.WEEKS.between(projFile.getCreationDate(), relDate);
		if(!this.ageList.containsKey(projFile.getName())) {
			this.ageList.put(projFile.getName(), weeks);
		}
		else {
			this.ageList.replace(projFile.getName(), weeks);
		}
	}
	
	
	// reset to 0s all the metrics that are valid for the single release
	private void resetVersMetrics() {
		for(String key : this.mapKeys) {
			this.churnList.replace(key, 0);
			this.locAddedList.replace(key, 0);
			this.nAuthList.replace(key, 0);
			this.maxLocAddList.replace(key, 0);
			this.nRev.replace(key, 0);
		}
	}
	
	
	/* Fill the Map containing info for the buggyness of the classes
	 * in a specific version, using the AV reported by Jira tickets
	 * 
	 * @param version: the current version
	 * @param versionDate: release date of the version
	 * @param prevDate: date of the previous release */
	private void setBuggynessWithAv(String version) {
		var buggy = false;
		var written = false;
		for(JavaFile jFile : this.projFiles) {
			for(RevCommit rc : jFile.getCommitList()) {
				var ld = (rc.getAuthorIdent().getWhen().toInstant()
						.atZone(ZoneId.systemDefault())).toLocalDate();
				if(ld.isAfter(this.versions.get(version)))
					buggy = checkIfBuggy(rc, version);
					
				// we found that the class was buggy
				if(buggy) {
					setBuggyInList(jFile.getName(), buggy);
					written = true;
					break;
				}
			}
			if(!written)
				setBuggyInList(jFile.getName(), buggy);
			written = false;
		}
	}
	
	
	/* Check if the commit refers to a "Bug" type ticket from
	 * Jira, if so check if among the affected versions of that ticket
	 * there is the current one
	 * 
	 * @param rc: the commit
	 * @param version: the current version name*/
	private boolean checkIfBuggy(RevCommit rc, String version) {
		for(Ticket tick : this.jiraTicketList) {

			// the commit refers to a ticket
			if((rc.getFullMessage().indexOf(tick.getKey()) != -1)) {
				return computeBuggyness(tick, rc, version);
			}
		}
		return false;
	}
	
	
	/* Check if AV are available for the given ticket, if so use them to 
	 * determine weather the class was buggy or not. Otherwise, apply proportion
	 * 
	 * @param tick: Jira ticket
	 * @param rc: git commit
	 * @param version: the release currently under analysis */
	private boolean computeBuggyness(Ticket tick, RevCommit rc, String version) {
		if(tick.getAvs().isEmpty() || tick.getFvs().isEmpty() || (tick.getAvs().isEmpty()
				&& !tick.getFvs().isEmpty()) || (tick.getAvs().isEmpty() && 
						tick.getFvs().isEmpty())) {
			return applyProportion(version, rc.getAuthorIdent().getWhen().toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDate(), tick);
		}
		else if(!tick.getAvs().isEmpty()){
			for(Version v : tick.getAvs()) {
				if(v.getName().contentEquals(version)) {
					incrementProportion(rc.getAuthorIdent().getWhen().toInstant()
							.atZone(ZoneId.systemDefault())
							.toLocalDate(), tick.getCrDateAsDate(),
							tick.getAvs(), tick.getFvs());
					return true;
				}
			}
		}
		return false;
	}
	
	
	/* Insert the record in the buggyness Map
	 * 
	 * @param: javaFileName: the name of the .java class
	 * @param: isBuggy: the state of the class*/
	private void setBuggyInList(String javaFileName, boolean isBuggy) {
		if(this.sizeList.get(javaFileName) <= 0)
			return;
		var bug = "no";
		if(isBuggy) {
			bug = "yes";
		}
		if(!this.buggynessMap.containsKey(javaFileName)) {
			this.buggynessMap.put(javaFileName, bug);
		}
		else {
			this.buggynessMap.replace(javaFileName, bug);
		}
			
	}
	
	
	/* Try to estimate the IV of a Bug using the Proportion method. Returns 
	 * true if the release is in the range: estimated IV - OV, where the OV is 
	 * computed as the first release which contains the ticket, given by
	 * the ticket opening date
	 * 
	 * @param release: current release under analysis
	 * @param fixCommDate: date of the fix commit
	 * @param tickOpenDate: opening date of the ticket */
	private boolean applyProportion(String release, LocalDate fixCommDate, Ticket tick) {
		String ov = null;
		String fv = null;
		String predIv;
		
		List<String> versionsId = new ArrayList<>();
		versionsId.addAll(this.versions.keySet());
		var tickOpenDate = tick.getCrDateAsDate();
		
		//find the opening version
		for(String ver : versionsId) {
			if(tickOpenDate.isBefore(this.versions.get(ver))) {
				ov = ver;
				break;
			}
		}
		
		var fvList = tick.getFvs();
		if(!fvList.isEmpty()) {
			fv = findFv(fvList, fixCommDate);
		}
		else {
			findFvByAllVers(versionsId, fixCommDate);
		}
		
		var ovInd = versionsId.indexOf(ov);
		var fvInd = versionsId.indexOf(fv);
		var predIvInd =  (int)Math.floor((fvInd - (fvInd - ovInd)*(this.proportion/this.nBuggy
				+this.nProp)));
		
		// the ticket is not consistent
		if(predIvInd > ovInd || ovInd > fvInd) {
			return false;
		}
		
		if(predIvInd < 0) {
			predIvInd = 0;
		}
		predIv = versionsId.get(predIvInd);
		
		LocalDate currRel = versions.get(release);
		LocalDate ivDate = versions.get(predIv);
		LocalDate ovDate = versions.get(ov);
		
		if(((currRel.isAfter(ivDate) || currRel.isEqual(ivDate)) && 
				currRel.isBefore(ovDate))) {
			this.nProp++;
			return true;
		}
		return false;
	}
	
	
	
	/* Find the Fix Version for a commit, needed to apply proportion
	 * 
	 * @param fvList: list of fixed versions for the ticket
	 * @param fixCommDate: date of the fix commit corresponding to the ticket 
	 * 
	 * N.B: this method was added to reduce the cognitive complexity of applyProportion()*/
	private String findFv(List<Version> fvList, LocalDate fixCommDate) {
		for(Version fixV : fvList) {
			if(fixCommDate.isBefore(this.versions.get(fixV.getName()))) {
				return fixV.getName();
			}
		}
		return null;
	}
	
	
	/* Find the Fix Version for a commit using the list of all the
	 * releases, needed to apply proportion
	 * 
	 * @param versionsId: list of all the releases' id
	 * @param fixCommDate: date of the fix commit corresponding to the ticket 
	 * 
	 * N.B: this method was added to reduce the cognitive complexity of applyProportion()*/
	private String findFvByAllVers(List<String> versionsId, LocalDate fixCommDate) {
		for(String ver : versionsId) {
			if(fixCommDate.isBefore(this.versions.get(ver))) {
				return ver;
			}
		}
		return null;
	}
	
	
	/* Increment the value of proportion, setting it as the average of
	 * defects fixed in previous versions
	 * 
	 * @param fixCommitDate: date of the commit corresponding to the Bug ticket
	 * @param tickCr: creation date of the ticket
	 * @param avList: list of the AV from the ticket
	 * @param fixVersionList: list of the FV from the ticket*/
	private void incrementProportion(LocalDate fixCommitDate, LocalDate tickCr,
			List<Version> avList, List<Version> fixVersionList) {
		
		String iv;
		var ivIndex = -1;
		var ovIndex = 0;
		String fv = null;
		
		List<String> vers = new ArrayList<>();
		vers.addAll(this.versions.keySet());
		
		// set the IV
		for(Version v : avList) {
			iv = v.getName();
			if(ivIndex == -1 || vers.indexOf(iv) <= ivIndex)
				ivIndex = vers.indexOf(iv);
		}
		
		ovIndex = getOpeningVers(vers, avList, tickCr);
		
		if(fixVersionList.isEmpty()){
			List<String> allVers = new ArrayList<>();
			allVers.addAll(this.versions.keySet());
			fv = this.findFvByAllVers(allVers, fixCommitDate);
		}
		else {
			//set the FV
			for(Version fVers : fixVersionList) {
				if(fVers.getReleaseDate().isAfter(fixCommitDate)) {
					fv = fVers.getName();
					break;
				}
			}
		}
		
		var fvIndex = vers.indexOf(fv);
		
		/*update proportion, avoiding division by zero 
		 * and checking if the ticket is consistent */
		if(fvIndex != ovIndex && fvIndex > ovIndex && ovIndex > ivIndex) {
			this.proportion += (fvIndex - ivIndex)/(float)(fvIndex - ovIndex);
		}
	}
	
	
	/* Get the opening version for a bug, using Jira information: if the Affected Versions are
	 * reported, use the latest as the OV, otherwise use the ticket creation's date
	 * 
	 * @param vers: list of all the releases
	 * @param avList: list of Affected Versions
	 * @param tickCr: creation date of the ticket 
	 * 
	 * N:B: method added to reduce the cognitive complexity of incrementProportion() */
	private int getOpeningVers(List<String> vers, List<Version> avList, LocalDate tickCr) {
		var ovIndex = 0;
		String ov;
		
		if(avList.size() > 1) {
			ovIndex = -1;
			for(Version v : avList) {
				ov = v.getName();
				if(ovIndex == -1 || vers.indexOf(ov) > ovIndex)
					ovIndex = vers.indexOf(ov);
			}
		}
		else {
			//set the OV
			for(String key : vers) {
				if(tickCr.isBefore(this.versions.get(key))) {
					ovIndex = vers.indexOf(key);
					break;
				}
			}
		}
		return ovIndex;
	}
	
	
	private void deleteIfExists() {
		var file = new File(this.project + constName);
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			Logger.getLogger("CSV_PROD").log(Level.SEVERE, "deleteIfExists(): error while deleting"
					+ "file");
		}
	}
}
