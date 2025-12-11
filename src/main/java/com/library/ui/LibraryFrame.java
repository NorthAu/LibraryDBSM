package com.library.ui;

import com.library.model.Book;
import com.library.model.BookDetail;
import com.library.model.Category;
import com.library.model.LoanDetail;
import com.library.model.Publisher;
import com.library.model.Reader;
import com.library.repository.LibraryRepository;
import com.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class LibraryFrame extends JFrame {
    private final LibraryService service;

    private final DefaultTableModel bookTableModel = new DefaultTableModel(new String[]{
            "ID", "ISBN", "书名", "分类", "出版社", "出版日期", "总数", "在册数"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel readerTableModel = new DefaultTableModel(new String[]{
            "ID", "姓名", "借阅证号", "到期日", "欠费"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel loanTableModel = new DefaultTableModel(new String[]{
            "借阅ID", "图书", "读者", "借出日", "到期日", "归还日", "续借次数", "已付罚金"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private JComboBox<Category> categoryCombo;
    private JComboBox<Publisher> publisherCombo;
    private JComboBox<Reader> borrowReaderCombo;
    private JComboBox<BookDetail> borrowBookCombo;

    private List<BookDetail> bookCache = Collections.emptyList();
    private List<Reader> readerCache = Collections.emptyList();
    private List<LoanDetail> loanCache = Collections.emptyList();

    public LibraryFrame() {
        this.service = new LibraryService(new LibraryRepository(), 1.5);
        setTitle("学校图书借阅管理系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("图书管理", createBookPanel());
        tabs.addTab("读者管理", createReaderPanel());
        tabs.addTab("借阅/归还", createLoanPanel());
        add(tabs, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            try {
                reloadLookups();
                reloadBooks();
                reloadReaders();
                reloadLoans();
            } catch (Exception e) {
                showError("初始化数据失败: " + e.getMessage());
            }
        });
    }

    private JPanel createBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(bookTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(3, 1));
        form.setBorder(BorderFactory.createTitledBorder("新增/更新图书"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField isbnField = new JTextField(12);
        JTextField titleField = new JTextField(15);
        JTextField publishDateField = new JTextField(10);
        row1.add(new JLabel("ISBN:"));
        row1.add(isbnField);
        row1.add(new JLabel("书名:"));
        row1.add(titleField);
        row1.add(new JLabel("出版日期(yyyy-MM-dd):"));
        row1.add(publishDateField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoryCombo = new JComboBox<>();
        publisherCombo = new JComboBox<>();
        JTextField totalField = new JTextField(5);
        JTextField availableField = new JTextField(5);
        row2.add(new JLabel("分类:"));
        row2.add(categoryCombo);
        row2.add(new JLabel("出版社:"));
        row2.add(publisherCombo);
        row2.add(new JLabel("总册数:"));
        row2.add(totalField);
        row2.add(new JLabel("在册数:"));
        row2.add(availableField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField newCategoryField = new JTextField(10);
        JTextField newPublisherField = new JTextField(10);
        JButton addCategoryButton = new JButton("新增分类");
        JButton addPublisherButton = new JButton("新增出版社");
        JButton saveBookButton = new JButton("保存图书");
        JButton refreshButton = new JButton("刷新");

        addCategoryButton.addActionListener(e -> {
            String name = newCategoryField.getText().trim();
            if (name.isEmpty()) {
                showError("请输入分类名称");
                return;
            }
            try {
                long id = service.saveCategory(name);
                reloadLookups();
                selectCategory(id);
                newCategoryField.setText("");
                showInfo("分类已保存");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        addPublisherButton.addActionListener(e -> {
            String name = newPublisherField.getText().trim();
            if (name.isEmpty()) {
                showError("请输入出版社名称");
                return;
            }
            try {
                long id = service.savePublisher(name);
                reloadLookups();
                selectPublisher(id);
                newPublisherField.setText("");
                showInfo("出版社已保存");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        saveBookButton.addActionListener(e -> {
            try {
                String isbn = isbnField.getText().trim();
                String title = titleField.getText().trim();
                if (isbn.isEmpty() || title.isEmpty()) {
                    throw new IllegalArgumentException("ISBN 和书名不能为空");
                }
                Category category = (Category) categoryCombo.getSelectedItem();
                Publisher publisher = (Publisher) publisherCombo.getSelectedItem();
                if (category == null || publisher == null) {
                    throw new IllegalArgumentException("请选择分类和出版社");
                }

                LocalDate publishedDate = publishDateField.getText().isBlank() ? null : LocalDate.parse(publishDateField.getText().trim());
                int total = Integer.parseInt(totalField.getText().trim());
                int available = availableField.getText().isBlank() ? total : Integer.parseInt(availableField.getText().trim());
                if (available > total) {
                    throw new IllegalArgumentException("在册数不能大于总册数");
                }

                service.addBook(new Book(0, isbn, title, category.id(), publisher.id(), publishedDate, total, available));
                reloadBooks();
                showInfo("保存成功");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        refreshButton.addActionListener(e -> {
            try {
                reloadLookups();
                reloadBooks();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        row3.add(new JLabel("新分类:"));
        row3.add(newCategoryField);
        row3.add(addCategoryButton);
        row3.add(new JLabel("新出版社:"));
        row3.add(newPublisherField);
        row3.add(addPublisherButton);
        row3.add(saveBookButton);
        row3.add(refreshButton);

        form.add(row1);
        form.add(row2);
        form.add(row3);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(readerTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.setBorder(BorderFactory.createTitledBorder("新增读者"));
        JTextField nameField = new JTextField(10);
        JTextField cardNumberField = new JTextField(10);
        JTextField expiryField = new JTextField(10);
        JButton addButton = new JButton("保存读者");
        JButton refreshButton = new JButton("刷新");

        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String cardNumber = cardNumberField.getText().trim();
                LocalDate expiry = LocalDate.parse(expiryField.getText().trim());
                if (name.isEmpty() || cardNumber.isEmpty()) {
                    throw new IllegalArgumentException("姓名和借阅证号不能为空");
                }
                service.addReader(new Reader(0, name, cardNumber, expiry, 0));
                reloadReaders();
                reloadLookups();
                showInfo("保存成功");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        refreshButton.addActionListener(e -> {
            try {
                reloadReaders();
                reloadLookups();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        form.add(new JLabel("姓名:"));
        form.add(nameField);
        form.add(new JLabel("借阅证号:"));
        form.add(cardNumberField);
        form.add(new JLabel("到期日(yyyy-MM-dd):"));
        form.add(expiryField);
        form.add(addButton);
        form.add(refreshButton);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLoanPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(loanTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(3, 1));
        controls.setBorder(BorderFactory.createTitledBorder("借阅/续借/归还"));

        // 借阅
        JPanel borrowRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        borrowReaderCombo = new JComboBox<>();
        borrowBookCombo = new JComboBox<>();
        JTextField dueDateField = new JTextField(10);
        JButton borrowButton = new JButton("借阅");
        borrowButton.addActionListener(e -> {
            try {
                Reader reader = (Reader) borrowReaderCombo.getSelectedItem();
                BookDetail book = (BookDetail) borrowBookCombo.getSelectedItem();
                if (reader == null || book == null) {
                    throw new IllegalArgumentException("请选择读者和可借阅的图书");
                }
                LocalDate dueDate = LocalDate.parse(dueDateField.getText().trim());
                service.borrowBook(reader.id(), book.id(), dueDate);
                reloadLoans();
                reloadBooks();
                showInfo("借阅成功");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        borrowRow.add(new JLabel("读者:"));
        borrowRow.add(borrowReaderCombo);
        borrowRow.add(new JLabel("图书:"));
        borrowRow.add(borrowBookCombo);
        borrowRow.add(new JLabel("到期日(yyyy-MM-dd):"));
        borrowRow.add(dueDateField);
        borrowRow.add(borrowButton);

        // 续借
        JPanel renewRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField renewLoanField = new JTextField(6);
        JTextField renewDateField = new JTextField(10);
        JButton renewButton = new JButton("续借");
        renewButton.addActionListener(e -> {
            try {
                long loanId = parseLongField(renewLoanField.getText(), "借阅ID");
                LocalDate newDate = LocalDate.parse(renewDateField.getText().trim());
                service.renewLoan(loanId, newDate);
                reloadLoans();
                showInfo("续借成功");
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        renewRow.add(new JLabel("借阅ID:"));
        renewRow.add(renewLoanField);
        renewRow.add(new JLabel("新到期日(yyyy-MM-dd):"));
        renewRow.add(renewDateField);
        renewRow.add(renewButton);

        // 归还
        JPanel returnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField returnLoanField = new JTextField(6);
        JTextField returnDateField = new JTextField(10);
        JButton returnButton = new JButton("归还");
        returnButton.addActionListener(e -> {
            try {
                int selected = table.getSelectedRow();
                LoanDetail detail;
                if (selected >= 0 && selected < loanCache.size()) {
                    detail = loanCache.get(selected);
                } else {
                    long loanId = parseLongField(returnLoanField.getText(), "借阅ID");
                    detail = loanCache.stream().filter(l -> l.id() == loanId).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("未找到对应的借阅记录"));
                }
                LocalDate returnedDate = LocalDate.parse(returnDateField.getText().trim());
                double fine = service.returnBook(detail.id(), detail.dueDate(), returnedDate);
                reloadLoans();
                reloadBooks();
                showInfo("归还成功，罚金：" + fine);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        returnRow.add(new JLabel("借阅ID(可在上表选中):"));
        returnRow.add(returnLoanField);
        returnRow.add(new JLabel("归还日期(yyyy-MM-dd):"));
        returnRow.add(returnDateField);
        returnRow.add(returnButton);

        controls.add(borrowRow);
        controls.add(renewRow);
        controls.add(returnRow);

        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.CENTER);
        JButton refreshButton = new JButton("刷新借阅列表");
        refreshButton.addActionListener(e -> {
            try {
                reloadLoans();
                reloadBooks();
                reloadLookups();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        south.add(refreshButton, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void reloadLookups() throws SQLException {
        List<Category> categories = service.listCategories();
        List<Publisher> publishers = service.listPublishers();
        readerCache = service.listReaders();
        bookCache = service.listBooks();

        categoryCombo.setModel(new DefaultComboBoxModel<>(categories.toArray(Category[]::new)));
        publisherCombo.setModel(new DefaultComboBoxModel<>(publishers.toArray(Publisher[]::new)));
        borrowReaderCombo.setModel(new DefaultComboBoxModel<>(readerCache.toArray(Reader[]::new)));
        borrowBookCombo.setModel(new DefaultComboBoxModel<>(bookCache.stream()
                .filter(b -> b.availableCopies() > 0)
                .toArray(BookDetail[]::new)));
    }

    private void reloadBooks() throws SQLException {
        bookCache = service.listBooks();
        bookTableModel.setRowCount(0);
        for (BookDetail book : bookCache) {
            bookTableModel.addRow(new Object[]{
                    book.id(),
                    book.isbn(),
                    book.title(),
                    book.categoryName(),
                    book.publisherName(),
                    book.publishedDate(),
                    book.totalCopies(),
                    book.availableCopies()
            });
        }
        borrowBookCombo.setModel(new DefaultComboBoxModel<>(bookCache.stream()
                .filter(b -> b.availableCopies() > 0)
                .toArray(BookDetail[]::new)));
    }

    private void reloadReaders() throws SQLException {
        readerCache = service.listReaders();
        readerTableModel.setRowCount(0);
        for (Reader reader : readerCache) {
            readerTableModel.addRow(new Object[]{
                    reader.id(),
                    reader.name(),
                    reader.cardNumber(),
                    reader.cardExpiry(),
                    reader.outstandingFine()
            });
        }
        borrowReaderCombo.setModel(new DefaultComboBoxModel<>(readerCache.toArray(Reader[]::new)));
    }

    private void reloadLoans() throws SQLException {
        loanCache = service.listLoanDetails();
        loanTableModel.setRowCount(0);
        for (LoanDetail loan : loanCache) {
            loanTableModel.addRow(new Object[]{
                    loan.id(),
                    loan.bookTitle(),
                    loan.readerName(),
                    loan.borrowedDate(),
                    loan.dueDate(),
                    loan.returnedDate(),
                    loan.renewals(),
                    loan.finePaid()
            });
        }
    }

    private void selectCategory(long id) {
        for (int i = 0; i < categoryCombo.getItemCount(); i++) {
            if (categoryCombo.getItemAt(i).id() == id) {
                categoryCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void selectPublisher(long id) {
        for (int i = 0; i < publisherCombo.getItemCount(); i++) {
            if (publisherCombo.getItemAt(i).id() == id) {
                publisherCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private long parseLongField(String value, String label) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "格式不正确");
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
