package br.com.portalmanager.platform.workspace.entrypoint.workspace.support;

import java.util.List;

/** Raw JSON cases used only when the HTTP contract itself requires a body that cannot bind to the request DTO. */
public final class WorkspaceProtocolPayloads {

    private WorkspaceProtocolPayloads() {
    }

    public static List<String> incompatibleCreateShapes() {
        String validBody = "{\"workspaceType\":\"ADMIN\",\"version\":null,\"name\":\"Workspace Protocol\","
                + "\"description\":\"Valid workspace description\",\"requester\":\"test-user\","
                + "\"acronym\":\"WSP\",\"authorizerGroup\":null,\"settings\":{},"
                + "\"emailGroup\":\"workspace@example.com\","
                + "\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\"}],"
                + "\"tags\":[]}";
        return List.of(
                validBody.replace("\"name\":\"Workspace Protocol\"", "\"name\":123"),
                validBody.replace("\"settings\":{}", "\"settings\":[\"invalid\"]"),
                validBody.replace("\"settings\":{}", "\"settings\":\"not-an-object\""),
                validBody.replace("\"tags\":[]", "\"tags\":\"invalid\""),
                validBody.replace("\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\"}]",
                        "\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\",\"unexpected\":true}]"),
                validBody.replace("\"tags\":[]}", "\"tags\":[],\"unexpected\":true}"));
    }

    public static String updateWithVersion(String name, String workspaceType, String rawVersion) {
        return "{\"version\":" + rawVersion + ",\"workspaceType\":\"" + workspaceType
                + "\",\"name\":\"" + name + "\",\"description\":\"Valid workspace description\","
                + "\"requester\":\"test-user\",\"acronym\":\"WSP\",\"authorizerGroup\":null,"
                + "\"settings\":{},\"emailGroup\":\"workspace@example.com\","
                + "\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\"}],"
                + "\"tags\":[]}";
    }

    public static List<String> incompatibleUpdateShapes(String name, String workspaceType, long version) {
        String validBody = updateWithVersion(name, workspaceType, Long.toString(version));
        return List.of(
                validBody.replace("\"name\":\"" + name + "\"", "\"name\":123"),
                validBody.replace("\"settings\":{}", "\"settings\":[\"invalid\"]"),
                validBody.replace("\"tags\":[]", "\"tags\":\"invalid\""),
                validBody.replace("\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\"}]",
                        "\"approvers\":[{\"functional\":\"F1000\",\"email\":\"approver@example.com\",\"unexpected\":true}]"),
                validBody.replace("\"tags\":[]}", "\"tags\":[],\"unexpected\":true}"));
    }
}
