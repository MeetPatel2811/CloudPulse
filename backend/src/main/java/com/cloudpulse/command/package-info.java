/*
 * Command pattern for user and system actions.
 *
 * Every action is a MonitoringCommand with one execute() method, so any caller
 * can run it the same way. The commands are:
 *   - RunHealthCheckCommand - check a service and process the result
 *   - AcknowledgeAlertCommand - acknowledge an alert
 *   - ResolveAlertCommand - resolve an alert
 *   - AssignIncidentOwnerCommand - assign an owner to an incident
 *   - AddIncidentNoteCommand - add a note to an incident's timeline
 *   - EnableMonitoringCommand - turn monitoring on for a service
 *   - DisableMonitoringCommand - turn monitoring off for a service
 */
package com.cloudpulse.command;
