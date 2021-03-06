<!DOCTYPE html>
<head>
  <title>Build report</title>
  <style type="text/css">
    body
    {
      margin: 0px;
      padding: 15px;
    }

    body, td, th
    {
      font-family: "Lucida Grande", "Lucida Sans Unicode", Helvetica, Arial, Tahoma, sans-serif;
      font-size: 10pt;
    }

    th
    {
      text-align: left;
    }

    h1
    {
      margin-top: 0px;
    }

    li
    {
      line-height: 15pt;
    }

    .change-add
    {
      color: #272;
    }

    .change-delete
    {
      color: #722;
    }

    .change-edit
    {
      color: #247;
    }

    .grayed
    {
      color: #AAA;
    }

    .error
    {
      color: #A33;
    }

    pre.console
    {
      color: #333;
      font-family: "Lucida Console", "Courier New";
      padding: 5px;
      line-height: 15px;
      background-color: #EEE;
      border: 1px solid #DDD;
    }
  </style>
</head>
<body>

<h1>Build ${build.result}</h1>
<table>
  <tr><th>Build URL:</th><td><a href="${rooturl}${build.url}">${rooturl}${build.url}</a></td></tr>
  <tr><th>Project:</th><td>${project.name}</td></tr>
  <tr><th>Date of build:</th><td>${it.timestampString}</td></tr>
  <tr><th>Build duration:</th><td>${build.durationString}</td></tr>
</table>

<!-- CHANGE SET -->
<% changeSet = build.changeSet
if (changeSet != null) {
  hadChanges = false %>
  <h2>Changes</h2>
  <ul>
<% 	changeSet.each { cs ->
    hadChanges = true
    aUser = cs.author %>
      <li>Commit <b>${cs.revision}</b> by <b><%= aUser != null ? aUser.displayName : it.author.displayName %>:</b> (${cs.msg})
        <ul>
<%        cs.affectedFiles.each { %>
          <li class="change-${it.editType.name}"><b>${it.editType.name}</b>: ${it.path}</li>
<%        } %>
        </ul>
      </li>
<%  }

  if (!hadChanges) { %>	
      <li>No Changes</li>
<%  } %>
  </ul>
<% } %>

<!-- ARTIFACTS -->
<% artifacts = build.artifacts
if (artifacts != null && artifacts.size() > 0) { %>
  <h2>Build artifacts</h2>
    <ul>
<%    artifacts.each() { f -> %>		
      <li><a href="${rooturl}${build.url}artifact/${f}">${f}</a></li>
<%    } %>
    </ul>
<% } %>

<% 
  testResult = build.testResultAction

  if (testResult) {
    jobName = build.parent.name
    rootUrl = hudson.model.Hudson.instance.rootUrl
    testResultsUrl = "${rootUrl}${build.url}testReport/"

    lastBuildSuccessRate = String.format("%.2f", (testResult.totalCount - testResult.result.failCount) * 100f / testResult.totalCount)
    lastBuildDuration = String.format("%.2f", testResult.result.duration)

    startedPassing = []
    startedFailing = []
    failing = []

    previousFailedTestCases = new HashSet()
    currentFailedTestCase = new HashSet()

    if (build.previousBuild?.testResultAction) {
      build.previousBuild.testResultAction.failedTests.each {
        previousFailedTestCases << it.simpleName + "." + it.safeName
      }
    }

    testResult.failedTests.each { tr ->
        packageName = tr.packageName
        className = tr.simpleName
        testName = tr.safeName
        displayName = className + "." + testName
        
        currentFailedTestCase << displayName
        url = "${rootUrl}${build.url}testReport/$packageName/$className/$testName"
        if (tr.age == 1) {
          startedFailing << [displayName: displayName, url: url, age: 1]
        } else {
          failing << [displayName: displayName, url: url, age: tr.age]
        }
    }

    startedPassing = previousFailedTestCases - currentFailedTestCase
    startedFailing = startedFailing.sort {it.displayName}
    failing = failing.sort {it.displayName}
    startedPassing = startedPassing.sort()
%>

<% if (testResult) { %>
<h2>Test Results</h2>
<ul>
  <li>Total tests ran: <a href="${testResultsUrl}">${testResult.totalCount}</a></li>
  <li>Failure count and delta: ${testResult.failCount} ${testResult.failureDiffString}</li>
  <li>Success rate: ${lastBuildSuccessRate}% </li>
</ul> 
<% } %>

<% if (startedPassing) { %>
<h3>Following tests started passing. Good work!</h3>
<ul>
  <% startedPassing.each { %>
    <li>${it}</li>
  <% } %>
</ul>
<% } %>

<% if (startedFailing) { %>
<h3>Following tests started FAILING. Have the last change caused it!!</h3>
<ul>
  <% startedFailing.each { %>
    <li><a href="${it.url}">${it.displayName}</a></li>
  <% } %>
</ul>
<% } %>

<% if (failing) { %>
<h3>Following tests are conitnuously failing. Someone should look into it!!!</h3>
<ul>
  <% failing.each { %>
    <li><a href="${it.url}">${it.displayName}</a> (Failing since ${it.age} runs)</li>
  <% } %>
</ul>
<% } %>

<% } %>

<!-- BUILD FAILURE REASONS -->
<% if (build.result == hudson.model.Result.FAILURE) {
  log = build.getLog(100).join("\n")
  warningsResultActions = build.actions.findAll { it.class.simpleName == "WarningsResultAction" }

  if (warningsResultActions.size() > 0) { %>
    <h2>Build errors</h2>
    <ul>
    <% warningsResultActions.each {
        newWarnings = it.result.newWarnings
        if (newWarnings.size() > 0) {
          newWarnings.each {
            if (it.priority.toString() == "HIGH") { %>
              <li class="error">In <b>${it.fileName}</b> at line ${it.primaryLineNumber}: ${it.message}</li>
          <% }} %>
    <% }} %>
    </ul>
  <% } %>

<h2>Console output</h2>
<pre class="console">${log}</pre>

<% } %>

</body>