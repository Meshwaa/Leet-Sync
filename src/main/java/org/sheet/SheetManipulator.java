package org.sheet;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.sheet.utils.LeetCodeClient;
import org.sheet.utils.SheetsServiceUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SheetManipulator {
    final static String spreadsheetId = "your-spreadsheet-id";

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        Sheets service = SheetsServiceUtil.getSheetsService();
        var sheets = service.spreadsheets().get(spreadsheetId).execute().getSheets().stream().map(sheet -> sheet.getProperties().getTitle()).toList();

        for (var sheet : sheets) {
            String range = sheet + "!C6:C";
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
            List<List<Object>> statusList = LeetCodeClient.fetchLeetcodeStatuses(titleSlugs, "your-username");
            System.out.println(statusList);

            String updateRange = sheet + "!E6:E";
            ValueRange body = new ValueRange().setValues(statusList);
            service.spreadsheets().values()
                    .update(spreadsheetId, updateRange, body)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

    private static String fetchProblemName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
