// SPDX-License-Identifier: Apache-2.0
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class FindReplaceDialog {
    private JDialog dialog;
    private JTextField searchField;
    private JTextField replaceField;
    private JCheckBox caseSensitive;
    private JCheckBox entireWord;
    private JCheckBox grepMode;
    private JCheckBox selectedOnly;
    private JCheckBox wrapAround;
    private RSyntaxTextArea editor;
    private int lastFoundPos = -1;

    // Captured selection range when "Selected Text Only" is active
    private int selRangeStart = -1;
    private int selRangeEnd = -1;

    public FindReplaceDialog(JFrame parent, RSyntaxTextArea editor) {
        this.editor = editor;
        dialog = new JDialog(parent, "Find / Replace", false);
        dialog.setLayout(new BorderLayout(8, 8));

        // Left panel: fields and options
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        leftPanel.add(new JLabel("Search:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        searchField = new JTextField(24);
        leftPanel.add(searchField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        leftPanel.add(new JLabel("Replace:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        replaceField = new JTextField(24);
        leftPanel.add(replaceField, gbc);

        // Matching Options
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel matchPanel = new JPanel(new GridLayout(3, 1));
        matchPanel.setBorder(BorderFactory.createTitledBorder("Matching Options"));
        caseSensitive = new JCheckBox("Case Sensitive");
        entireWord = new JCheckBox("Entire Word");
        grepMode = new JCheckBox("Grep");
        matchPanel.add(caseSensitive);
        matchPanel.add(entireWord);
        matchPanel.add(grepMode);
        leftPanel.add(matchPanel, gbc);

        // Search In
        gbc.gridy = 3;
        JPanel searchInPanel = new JPanel(new GridLayout(2, 1));
        searchInPanel.setBorder(BorderFactory.createTitledBorder("Search In"));
        selectedOnly = new JCheckBox("Selected Text Only");
        wrapAround = new JCheckBox("Wrap Around", true);
        searchInPanel.add(selectedOnly);
        searchInPanel.add(wrapAround);
        leftPanel.add(searchInPanel, gbc);

        // Capture selection range when "Selected Text Only" is checked
        selectedOnly.addActionListener(e -> {
            if (selectedOnly.isSelected()) {
                captureSelectionRange();
            } else {
                selRangeStart = -1;
                selRangeEnd = -1;
            }
        });

        // Right panel: buttons
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 0, 4));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
        JButton nextBtn = new JButton("Next");
        JButton prevBtn = new JButton("Previous");
        JButton firstBtn = new JButton("First");
        JButton replaceBtn = new JButton("Replace");
        JButton replaceAllBtn = new JButton("Replace All");
        JButton replaceFindBtn = new JButton("Replace and Find");
        buttonPanel.add(nextBtn);
        buttonPanel.add(prevBtn);
        buttonPanel.add(firstBtn);
        buttonPanel.add(replaceBtn);
        buttonPanel.add(replaceAllBtn);
        buttonPanel.add(replaceFindBtn);

        nextBtn.addActionListener(e -> findNext());
        prevBtn.addActionListener(e -> findPrevious());
        firstBtn.addActionListener(e -> findFirst());
        replaceBtn.addActionListener(e -> replaceCurrent());
        replaceAllBtn.addActionListener(e -> replaceAll());
        replaceFindBtn.addActionListener(e -> { replaceCurrent(); findNext(); });

        dialog.add(leftPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.EAST);
        dialog.pack();
        dialog.getRootPane().setDefaultButton(nextBtn);
        dialog.setLocationRelativeTo(parent);
    }

    public void show() {
        dialog.setVisible(true);
        searchField.requestFocus();
        String sel = editor.getSelectedText();
        if (sel != null && !sel.contains("\n")) {
            searchField.setText(sel);
            searchField.selectAll();
        }
        // If "Selected Text Only" is already checked, recapture the range
        if (selectedOnly.isSelected()) {
            captureSelectionRange();
        }
    }

    /**
     * Capture the current editor selection as the restricted search range.
     */
    private void captureSelectionRange() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start != end) {
            selRangeStart = start;
            selRangeEnd = end;
        } else {
            // No selection — use entire document
            selRangeStart = 0;
            selRangeEnd = editor.getDocument().getLength();
        }
    }

    private Pattern buildPattern(String search) {
        int flags = caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
        String regex;
        if (grepMode.isSelected()) {
            regex = search;
        } else {
            regex = Pattern.quote(search);
        }
        if (entireWord.isSelected()) {
            regex = "\\b" + regex + "\\b";
        }
        return Pattern.compile(regex, flags);
    }

    /**
     * Returns the start and end offsets of the search range.
     * When "Selected Text Only" is active, returns the captured range.
     */
    private int[] getSearchRange() {
        if (selectedOnly.isSelected() && selRangeStart >= 0 && selRangeEnd > selRangeStart) {
            // Clamp to document length in case edits have shortened it
            int docLen = editor.getDocument().getLength();
            return new int[]{Math.min(selRangeStart, docLen), Math.min(selRangeEnd, docLen)};
        }
        return new int[]{0, editor.getDocument().getLength()};
    }

    private void findNext() {
        String search = searchField.getText();
        if (search.isEmpty()) return;
        try {
            int[] range = getSearchRange();
            String text = editor.getDocument().getText(0, editor.getDocument().getLength());
            Pattern p = buildPattern(search);
            int startFrom = editor.getCaretPosition();
            // Clamp startFrom to the search range
            if (startFrom < range[0]) startFrom = range[0];
            if (startFrom > range[1]) startFrom = range[1];

            // Search within [startFrom, range[1]]
            Matcher m = p.matcher(text);
            m.region(startFrom, range[1]);
            if (m.find()) {
                select(m.start(), m.end());
            } else if (wrapAround.isSelected()) {
                // Wrap: search from range start
                m.region(range[0], range[1]);
                if (m.find()) {
                    select(m.start(), m.end());
                }
            }
        } catch (BadLocationException ignored) {}
    }

    private void findPrevious() {
        String search = searchField.getText();
        if (search.isEmpty()) return;
        try {
            int[] range = getSearchRange();
            String text = editor.getDocument().getText(0, editor.getDocument().getLength());
            Pattern p = buildPattern(search);
            int endBefore = editor.getSelectionStart();
            // Clamp to range
            if (endBefore > range[1]) endBefore = range[1];
            if (endBefore < range[0]) endBefore = range[0];

            // Find the last match within [range[0], endBefore)
            Matcher m = p.matcher(text);
            m.region(range[0], range[1]);
            int lastStart = -1, lastEnd = -1;
            while (m.find() && m.start() < endBefore) {
                lastStart = m.start();
                lastEnd = m.end();
            }
            if (lastStart >= 0) {
                select(lastStart, lastEnd);
            } else if (wrapAround.isSelected()) {
                // Wrap: find the last match in the entire range
                m = p.matcher(text);
                m.region(range[0], range[1]);
                while (m.find()) { lastStart = m.start(); lastEnd = m.end(); }
                if (lastStart >= 0) select(lastStart, lastEnd);
            }
        } catch (BadLocationException ignored) {}
    }

    private void findFirst() {
        String search = searchField.getText();
        if (search.isEmpty()) return;
        try {
            int[] range = getSearchRange();
            String text = editor.getDocument().getText(0, editor.getDocument().getLength());
            Pattern p = buildPattern(search);
            Matcher m = p.matcher(text);
            m.region(range[0], range[1]);
            if (m.find()) select(m.start(), m.end());
        } catch (BadLocationException ignored) {}
    }

    private void replaceCurrent() {
        if (editor.getSelectedText() == null) return;
        // Verify the selection is within the search range
        int[] range = getSearchRange();
        int selStart = editor.getSelectionStart();
        int selEnd = editor.getSelectionEnd();
        if (selStart < range[0] || selEnd > range[1]) return;

        String replacement = replaceField.getText();
        if (grepMode.isSelected()) {
            try {
                Pattern p = buildPattern(searchField.getText());
                Matcher m = p.matcher(editor.getSelectedText());
                if (m.matches()) {
                    replacement = m.replaceFirst(replaceField.getText());
                }
            } catch (Exception ignored) {}
        }
        int lengthDiff = replacement.length() - (selEnd - selStart);
        editor.replaceSelection(replacement);

        // Adjust the captured selection range end to account for the replacement length change
        if (selectedOnly.isSelected() && selRangeEnd >= 0) {
            selRangeEnd += lengthDiff;
        }
    }

    private void replaceAll() {
        String search = searchField.getText();
        if (search.isEmpty()) return;
        try {
            int[] range = getSearchRange();
            String text = editor.getDocument().getText(0, editor.getDocument().getLength());
            Pattern p = buildPattern(search);
            String replacement = replaceField.getText();

            // Only replace within the range
            String before = text.substring(0, range[0]);
            String within = text.substring(range[0], range[1]);
            String after = text.substring(range[1]);

            Matcher m = p.matcher(within);
            String replaced = m.replaceAll(grepMode.isSelected() ? replacement : Matcher.quoteReplacement(replacement));

            editor.setText(before + replaced + after);

            // Update the captured range end to reflect the new length
            if (selectedOnly.isSelected() && selRangeEnd >= 0) {
                selRangeEnd = range[0] + replaced.length();
            }
        } catch (BadLocationException ignored) {}
    }

    private void select(int start, int end) {
        editor.setCaretPosition(start);
        editor.moveCaretPosition(end);
        editor.requestFocus();
    }
}
