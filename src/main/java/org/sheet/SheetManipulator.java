package org.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.sheet.utils.LeetCodeClient;
import org.sheet.utils.SheetsServiceUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SheetManipulator {
    final static String spreadsheetId = "1TUG8nPzN-GhOTH-PAVlWYYeLoKKd8CrorgEBAcD8NK8";

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        Sheets service = SheetsServiceUtil.getSheetsService();
        var sheets = service.spreadsheets().get(spreadsheetId).execute().getSheets();

        for (var sheet : sheets) {
            String range = sheet.getProperties().getTitle() + "!C6:C"; // Fetch problem url for each row
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            List<String> titleSlugs = new ArrayList<>();
            for (var row : values) {
                for (var cell : row) {
                    var problemName = fetchProblemName((String) cell);
                    titleSlugs.add(problemName);
                }
            }
            updateStatusInSheet(service, titleSlugs, sheet);
        }
    }

    private static void updateStatusInSheet(Sheets sheetsService, List<String> titleSlugs, Sheet sheet) throws IOException {
        // Fetch the status list using LeetCodeClient
        List<String> statusList = LeetCodeClient.fetchLeetcodeStatuses(titleSlugs, "Meshwa_p");
        System.out.println(statusList);

        List<Request> requests = new ArrayList<>();
        for (int i = 0; i < statusList.size(); i++) {
            Color color = getStatusColor(statusList.get(i));
            // Create format request to apply color
            requests.add(createTextFormatRequest(5 + i, color,sheet.getProperties().getSheetId()));
        }
        // Batch update the spreadsheet with formatting
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
        // Execute the batch update for formatting
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
    }

    private static Request createTextFormatRequest(int rowIndex, Color color,Integer sheetId) {
        TextFormat textFormat = new TextFormat().setForegroundColor(color);
        CellFormat cellFormat = new CellFormat().setTextFormat(textFormat);

        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId) // Adjust sheet ID as needed
                        .setStartRowIndex(rowIndex)
                        .setEndRowIndex(rowIndex + 1)
                        .setStartColumnIndex(4) // For E column
                        .setEndColumnIndex(5))
                .setCell(new CellData().setUserEnteredFormat(cellFormat))
                .setFields("userEnteredFormat.textFormat.foregroundColor"));
    }

    private static Color getStatusColor(String status) {
        Color color = new Color();

        switch (status) {
            case "Solved" -> color.setGreen(1.6f); // Green for "Solved"
            case "Attempted" -> color.setRed(0.8f).setGreen(0.5f); // Yellow for "Attempted"
            default -> color.setRed(1.0f); // Red for "Pending"
        }
        return color;
    }

    private static String fetchProblemName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
