import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class hospital_db extends JFrame {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/hospital_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "123456"; // <-- Pon aquí tu contraseña de PostgreSQL

    private final Map<String, Integer> pacientesMap = new HashMap<>();
    private final Map<String, Integer> empleadosMap = new HashMap<>();

    // Pestaña 1: Registrar Ingreso
    private JComboBox<String> cbPaciente;
    private JComboBox<String> cbDepartamento;
    private JComboBox<String> cbEmpleado;
    private JTextField txtCondicion;
    private JCheckBox chkAmbulatorio;
    private JTextField txtPeso;
    private JTextField txtAltura;
    private JComboBox<String> cbEstadoCuenta;
    private JComboBox<String> cbHabitacion;
    private JTextArea txtDescripcion;

    // Pestaña 2: Ingresos, Cargos y Alta
    private JComboBox<String> cbFiltroEstado;
    private DefaultTableModel modeloIngresos;
    private JTable tablaIngresos;
    private JTextField txtConcepto;
    private JTextField txtCantidad;
    private JTextField txtUnidad;
    private JTextField txtPrecio;

    // Pestaña 3: Nuevo Paciente y Dirección
    private JTextField txtPacNombre;
    private JTextField txtPacApellidos;
    private JComboBox<String> cbPacGenero;
    private JTextField txtPacFechaNac;
    private JTextField txtPacNss;
    private JTextField txtPacTelCel;
    private JTextField txtPacContactoNom;
    private JTextField txtPacContactoTel;
    private JTextField txtDirCalle;
    private JTextField txtDirCiudad;
    private JTextField txtDirCp;

    public hospital_db() {
        super("Admisiones Hospitalarias — hospital_db (Swing + PostgreSQL)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);

        initUI();
        cargarCatalogos();
        cargarTablaIngresos();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topHeader = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Sistema de Admisiones Hospitalarias");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JButton btnRefrescarTodo = new JButton("Actualizar Catálogos y Tablas");
        btnRefrescarTodo.addActionListener(e -> {
            cargarCatalogos();
            cargarTablaIngresos();
        });
        topHeader.add(lblTitle, BorderLayout.WEST);
        topHeader.add(btnRefrescarTodo, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Registrar Ingreso", crearPanelIngreso());
        tabs.addTab("2. Ingresos, Cargos y Alta", crearPanelGestion());
        tabs.addTab("3. Nuevo Paciente y Dirección", crearPanelPaciente());

        rootPanel.add(topHeader, BorderLayout.NORTH);
        rootPanel.add(tabs, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private void cargarCatalogos() {
        String prevPac = cbPaciente != null ? (String) cbPaciente.getSelectedItem() : null;
        String prevEmp = cbEmpleado != null ? (String) cbEmpleado.getSelectedItem() : null;
        String prevDep = cbDepartamento != null ? (String) cbDepartamento.getSelectedItem() : null;
        String prevHab = cbHabitacion != null ? (String) cbHabitacion.getSelectedItem() : null;

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            pacientesMap.clear();
            DefaultComboBoxModel<String> pacModel = new DefaultComboBoxModel<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT paciente_id, nombre, COALESCE(apellidos, ''), COALESCE(nss, 'Sin NSS') " +
                    "FROM pacientes ORDER BY paciente_id DESC")) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String label = (id + " — " + rs.getString(2) + " " + rs.getString(3)).trim()
                            + " [" + rs.getString(4) + "]";
                    pacientesMap.put(label, id);
                    pacModel.addElement(label);
                }
            }
            cbPaciente.setModel(pacModel);
            if (prevPac != null && pacientesMap.containsKey(prevPac)) {
                cbPaciente.setSelectedItem(prevPac);
            }

            empleadosMap.clear();
            DefaultComboBoxModel<String> empModel = new DefaultComboBoxModel<>();
            empModel.addElement("(Sin asignar)");
            try (ResultSet rs = st.executeQuery(
                    "SELECT empleado_id, nombre, apellidos, nombre_departamento, titulo_trabajo " +
                    "FROM empleados ORDER BY empleado_id")) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String label = id + " — " + rs.getString(2) + " " + rs.getString(3)
                            + " (" + rs.getString(4) + " / " + rs.getString(5) + ")";
                    empleadosMap.put(label, id);
                    empModel.addElement(label);
                }
            }
            cbEmpleado.setModel(empModel);
            if (prevEmp != null) {
                cbEmpleado.setSelectedItem(prevEmp);
            }

            DefaultComboBoxModel<String> depModel = new DefaultComboBoxModel<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT nombre_departamento FROM departamentos ORDER BY nombre_departamento")) {
                while (rs.next()) {
                    depModel.addElement(rs.getString(1));
                }
            }
            cbDepartamento.setModel(depModel);
            if (prevDep != null) {
                cbDepartamento.setSelectedItem(prevDep);
            }

            DefaultComboBoxModel<String> habModel = new DefaultComboBoxModel<>();
            habModel.addElement("(No asignar habitación)");
            try (ResultSet rs = st.executeQuery(
                    "SELECT num_habitacion, capacidad, estado_habitacion " +
                    "FROM habitaciones ORDER BY num_habitacion")) {
                while (rs.next()) {
                    habModel.addElement(rs.getString(1) + " (Cap: " + rs.getInt(2)
                            + ", " + rs.getString(3) + ")");
                }
            }
            cbHabitacion.setModel(habModel);
            if (prevHab != null) {
                cbHabitacion.setSelectedItem(prevHab);
            }
            actualizarEstadoComboHabitacion();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron cargar los catálogos:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Pestaña 1: Registrar Ingreso
    // -------------------------------------------------------------------------
    private JPanel crearPanelIngreso() {
        JPanel outer = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos del Episodio de Ingreso"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        cbPaciente = new JComboBox<>();
        cbDepartamento = new JComboBox<>();
        cbEmpleado = new JComboBox<>();
        txtCondicion = new JTextField(35);
        chkAmbulatorio = new JCheckBox("Atención ambulatoria (no ocupa habitación)");
        txtPeso = new JTextField(10);
        txtAltura = new JTextField(10);
        cbEstadoCuenta = new JComboBox<>(new String[]{"Sin cuenta", "Pendiente"});
        cbHabitacion = new JComboBox<>();
        txtDescripcion = new JTextArea(4, 35);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);

        chkAmbulatorio.addActionListener(e -> actualizarEstadoComboHabitacion());

        int r = 0;
        addRow(form, gbc, r++, "Paciente *:", cbPaciente, 2);
        addRow(form, gbc, r++, "Departamento *:", cbDepartamento, 1);
        addRow(form, gbc, r++, "Empleado responsable:", cbEmpleado, 2);
        addRow(form, gbc, r++, "Condición médica *:", txtCondicion, 2);
        addRow(form, gbc, r++, "Tipo de atención:", chkAmbulatorio, 2);

        gbc.gridy = r;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Peso (kg):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        form.add(txtPeso, gbc);

        JPanel altPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        altPanel.add(new JLabel("Altura (m, ej. 1.75):"));
        altPanel.add(txtAltura);
        gbc.gridx = 2;
        gbc.weightx = 0.7;
        form.add(altPanel, gbc);
        r++;

        addRow(form, gbc, r++, "Estado inicial de cuenta:", cbEstadoCuenta, 1);
        addRow(form, gbc, r++, "Habitación inicial:", cbHabitacion, 2);

        gbc.gridy = r;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Descripción / evolución *:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(txtDescripcion), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        r++;

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnGuardar = new JButton("Guardar Ingreso");
        btnLimpiar.addActionListener(e -> limpiarFormularioIngreso());
        btnGuardar.addActionListener(e -> guardarIngreso());
        btns.add(btnLimpiar);
        btns.add(btnGuardar);

        gbc.gridy = r;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        form.add(btns, gbc);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp, int colSpan) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = colSpan;
        gbc.weightx = 1;
        panel.add(comp, gbc);
        gbc.gridwidth = 1;
    }

    private void actualizarEstadoComboHabitacion() {
        if (chkAmbulatorio.isSelected()) {
            cbHabitacion.setSelectedIndex(0);
            cbHabitacion.setEnabled(false);
        } else {
            cbHabitacion.setEnabled(true);
        }
    }

    private void limpiarFormularioIngreso() {
        txtCondicion.setText("");
        chkAmbulatorio.setSelected(false);
        actualizarEstadoComboHabitacion();
        txtPeso.setText("");
        txtAltura.setText("");
        cbEstadoCuenta.setSelectedItem("Sin cuenta");
        if (cbHabitacion.getItemCount() > 0) {
            cbHabitacion.setSelectedIndex(0);
        }
        txtDescripcion.setText("");
    }

    private BigDecimal parseOptionalDecimal(String raw, String campo, double minExclusive, double maxInclusive) {
        String s = raw == null ? "" : raw.trim().replace(',', '.');
        if (s.isEmpty()) {
            return null;
        }
        try {
            BigDecimal val = new BigDecimal(s);
            if (val.doubleValue() <= minExclusive || val.doubleValue() > maxInclusive) {
                throw new NumberFormatException();
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    campo + " debe ser un número mayor a " + minExclusive + " y hasta " + maxInclusive + ".");
        }
    }

    private void guardarIngreso() {
        String pacSel = (String) cbPaciente.getSelectedItem();
        String depto = (String) cbDepartamento.getSelectedItem();
        String empSel = (String) cbEmpleado.getSelectedItem();
        String condicion = txtCondicion.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        boolean ambulatorio = chkAmbulatorio.isSelected();
        String estadoCuenta = (String) cbEstadoCuenta.getSelectedItem();
        String habSel = (String) cbHabitacion.getSelectedItem();

        if (pacSel == null || !pacientesMap.containsKey(pacSel)) {
            JOptionPane.showMessageDialog(this, "Selecciona un paciente válido.", "Falta dato", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (depto == null || depto.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecciona un departamento.", "Falta dato", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (condicion.isEmpty() || condicion.length() > 100) {
            JOptionPane.showMessageDialog(this, "La condición médica es obligatoria (máx. 100 caracteres).", "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (descripcion.isEmpty() || descripcion.length() > 100) {
            JOptionPane.showMessageDialog(this, "La descripción es obligatoria (máx. 100 caracteres).", "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal peso;
        BigDecimal altura;
        try {
            peso = parseOptionalDecimal(txtPeso.getText(), "Peso", 0.0, 500.0);
            altura = parseOptionalDecimal(txtAltura.getText(), "Altura", 0.0, 2.80);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int pacienteId = pacientesMap.get(pacSel);
        Integer empleadoId = empleadosMap.get(empSel);
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        Timestamp fechaEmision = "Pendiente".equals(estadoCuenta) ? ahora : null;

        String numHabitacion = null;
        if (!ambulatorio && habSel != null && !habSel.startsWith("(No asignar")) {
            numHabitacion = habSel.split(" ")[0].trim();
        }

        String insertIngresoSql =
                "INSERT INTO ingresos (" +
                "  paciente_id, nombre_departamento, empleado_id, condicion," +
                "  fecha_admision, fecha_alta, ambulatorio, peso, altura," +
                "  estado_ingreso, descripcion, estado_cuenta, fecha_emision, fecha_pago" +
                ") VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, 'Activo', ?, ?, ?, NULL) " +
                "RETURNING ingreso_id";

        String insertOcupacionSql =
                "INSERT INTO ocupaciones (ingreso_id, num_habitacion, fecha_entrada, fecha_salida) " +
                "VALUES (?, ?, ?, NULL)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int nuevoIngresoId;
                try (PreparedStatement ps = conn.prepareStatement(insertIngresoSql)) {
                    ps.setInt(1, pacienteId);
                    ps.setString(2, depto);
                    if (empleadoId != null) {
                        ps.setInt(3, empleadoId);
                    } else {
                        ps.setNull(3, Types.INTEGER);
                    }
                    ps.setString(4, condicion);
                    ps.setTimestamp(5, ahora);
                    ps.setBoolean(6, ambulatorio);
                    if (peso != null) {
                        ps.setBigDecimal(7, peso);
                    } else {
                        ps.setNull(7, Types.NUMERIC);
                    }
                    if (altura != null) {
                        ps.setBigDecimal(8, altura);
                    } else {
                        ps.setNull(8, Types.NUMERIC);
                    }
                    ps.setString(9, descripcion);
                    ps.setString(10, estadoCuenta);
                    if (fechaEmision != null) {
                        ps.setTimestamp(11, fechaEmision);
                    } else {
                        ps.setNull(11, Types.TIMESTAMP);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        nuevoIngresoId = rs.getInt(1);
                    }
                }

                if (numHabitacion != null) {
                    try (PreparedStatement psHab = conn.prepareStatement(insertOcupacionSql)) {
                        psHab.setInt(1, nuevoIngresoId);
                        psHab.setString(2, numHabitacion);
                        psHab.setTimestamp(3, ahora);
                        psHab.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this,
                        "Se creó el ingreso #" + nuevoIngresoId
                        + (numHabitacion != null ? " y se asignó la habitación " + numHabitacion + "." : "."),
                        "Ingreso registrado", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormularioIngreso();
                cargarTablaIngresos();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el ingreso:\n" + ex.getMessage(),
                    "Error en PostgreSQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Pestaña 2: Consulta de Ingresos, Cargos y Alta/Pago
    // -------------------------------------------------------------------------
    private JPanel crearPanelGestion() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Filtrar por estado:"));
        cbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Activo", "Finalizado", "Cancelado"});
        cbFiltroEstado.addActionListener(e -> cargarTablaIngresos());
        JButton btnRefrescar = new JButton("Refrescar lista");
        btnRefrescar.addActionListener(e -> cargarTablaIngresos());
        topBar.add(cbFiltroEstado);
        topBar.add(btnRefrescar);

        String[] columnas = {"ID", "Paciente", "Departamento", "Condición", "Ambul.", "Estado", "Cuenta", "Total ($)"};
        modeloIngresos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaIngresos = new JTable(modeloIngresos);
        tablaIngresos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel acciones = new JPanel(new BorderLayout(6, 8));
        acciones.setBorder(BorderFactory.createTitledBorder("Acciones sobre el Ingreso Seleccionado"));

        JPanel filaCargo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtConcepto = new JTextField(18);
        txtCantidad = new JTextField("1", 5);
        txtUnidad = new JTextField("Pieza", 8);
        txtPrecio = new JTextField(8);
        JButton btnCargo = new JButton("Agregar Cargo");
        btnCargo.addActionListener(e -> agregarCargo());

        filaCargo.add(new JLabel("Concepto:"));
        filaCargo.add(txtConcepto);
        filaCargo.add(new JLabel("Cant.:"));
        filaCargo.add(txtCantidad);
        filaCargo.add(new JLabel("Unidad:"));
        filaCargo.add(txtUnidad);
        filaCargo.add(new JLabel("P. Unitario:"));
        filaCargo.add(txtPrecio);
        filaCargo.add(btnCargo);

        JPanel filaEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPagar = new JButton("Marcar Cuenta Pagada");
        JButton btnAlta = new JButton("Dar de Alta (Finalizar)");
        btnPagar.addActionListener(e -> marcarCuentaPagada());
        btnAlta.addActionListener(e -> darDeAlta());
        filaEstado.add(btnPagar);
        filaEstado.add(btnAlta);

        acciones.add(filaCargo, BorderLayout.NORTH);
        acciones.add(filaEstado, BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaIngresos), BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private Integer getSelectedIngresoId() {
        int row = tablaIngresos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un ingreso de la tabla.", "Sin selección", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return Integer.parseInt(modeloIngresos.getValueAt(row, 0).toString());
    }

    private void cargarTablaIngresos() {
        modeloIngresos.setRowCount(0);
        String filtro = cbFiltroEstado != null ? (String) cbFiltroEstado.getSelectedItem() : "Todos";

        StringBuilder sql = new StringBuilder(
                "SELECT i.ingreso_id, " +
                "       TRIM(p.nombre || ' ' || COALESCE(p.apellidos, '')) AS paciente, " +
                "       i.nombre_departamento, " +
                "       i.condicion, " +
                "       CASE WHEN i.ambulatorio THEN 'Sí' ELSE 'No' END AS amb, " +
                "       i.estado_ingreso, " +
                "       i.estado_cuenta, " +
                "       COALESCE(c.total, 0) AS total " +
                "FROM ingresos i " +
                "JOIN pacientes p ON p.paciente_id = i.paciente_id " +
                "LEFT JOIN vw_cuentas c ON c.ingreso_id = i.ingreso_id ");

        boolean tieneFiltro = filtro != null && !"Todos".equals(filtro);
        if (tieneFiltro) {
            sql.append("WHERE i.estado_ingreso = ? ");
        }
        sql.append("ORDER BY i.ingreso_id DESC");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (tieneFiltro) {
                ps.setString(1, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(8);
                    modeloIngresos.addRow(new Object[]{
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6),
                            rs.getString(7),
                            String.format("$%,.2f", total != null ? total.doubleValue() : 0.0)
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron cargar los ingresos:\n" + ex.getMessage(),
                    "Error al consultar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarCargo() {
        Integer ingresoId = getSelectedIngresoId();
        if (ingresoId == null) {
            return;
        }

        String concepto = txtConcepto.getText().trim();
        String unidad = txtUnidad.getText().trim().isEmpty() ? "Pieza" : txtUnidad.getText().trim();
        BigDecimal cantidad;
        BigDecimal precio;
        try {
            cantidad = new BigDecimal(txtCantidad.getText().trim().replace(',', '.'));
            precio = new BigDecimal(txtPrecio.getText().trim().replace(',', '.'));
            if (concepto.isEmpty() || cantidad.compareTo(BigDecimal.ZERO) <= 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Revisa concepto, cantidad (> 0) y precio unitario (>= 0).",
                    "Dato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String updCuenta =
                "UPDATE ingresos " +
                "SET estado_cuenta = 'Pendiente', fecha_emision = COALESCE(fecha_emision, NOW()) " +
                "WHERE ingreso_id = ? AND estado_cuenta = 'Sin cuenta'";
        String insCargo =
                "INSERT INTO cargos (ingreso_id, concepto, cantidad, unidad_medida, precio_unitario) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psUpd = conn.prepareStatement(updCuenta)) {
                    psUpd.setInt(1, ingresoId);
                    psUpd.executeUpdate();
                }
                try (PreparedStatement psIns = conn.prepareStatement(insCargo)) {
                    psIns.setInt(1, ingresoId);
                    psIns.setString(2, concepto);
                    psIns.setBigDecimal(3, cantidad);
                    psIns.setString(4, unidad);
                    psIns.setBigDecimal(5, precio);
                    psIns.executeUpdate();
                }
                conn.commit();
                txtConcepto.setText("");
                txtPrecio.setText("");
                txtCantidad.setText("1");
                cargarTablaIngresos();
                JOptionPane.showMessageDialog(this, "Cargo registrado en el ingreso #" + ingresoId + ".");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo agregar el cargo:\n" + ex.getMessage(),
                    "Error en PostgreSQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void marcarCuentaPagada() {
        Integer ingresoId = getSelectedIngresoId();
        if (ingresoId == null) {
            return;
        }

        String sql =
                "UPDATE ingresos " +
                "SET estado_cuenta = 'Pagada', " +
                "    fecha_emision = COALESCE(fecha_emision, NOW()), " +
                "    fecha_pago = NOW() " +
                "WHERE ingreso_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingresoId);
            ps.executeUpdate();
            cargarTablaIngresos();
            JOptionPane.showMessageDialog(this, "La cuenta del ingreso #" + ingresoId + " se marcó como Pagada.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo actualizar la cuenta:\n" + ex.getMessage(),
                    "Error en PostgreSQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void darDeAlta() {
        Integer ingresoId = getSelectedIngresoId();
        if (ingresoId == null) {
            return;
        }

        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        String updIngreso =
                "UPDATE ingresos " +
                "SET estado_ingreso = 'Finalizado', fecha_alta = COALESCE(fecha_alta, ?) " +
                "WHERE ingreso_id = ?";
        String updOcupaciones =
                "UPDATE ocupaciones " +
                "SET fecha_salida = COALESCE(fecha_salida, ?) " +
                "WHERE ingreso_id = ? AND fecha_salida IS NULL";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(updIngreso)) {
                    ps1.setTimestamp(1, ahora);
                    ps1.setInt(2, ingresoId);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement(updOcupaciones)) {
                    ps2.setTimestamp(1, ahora);
                    ps2.setInt(2, ingresoId);
                    ps2.executeUpdate();
                }
                conn.commit();
                cargarTablaIngresos();
                JOptionPane.showMessageDialog(this, "El ingreso #" + ingresoId + " se marcó como Finalizado.");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo dar de alta:\n" + ex.getMessage(),
                    "Error en PostgreSQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------------------
    // Pestaña 3: Nuevo Paciente y Dirección Vigente
    // -------------------------------------------------------------------------
    private JPanel crearPanelPaciente() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Alta de Paciente y Domicilio Vigente"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        txtPacNombre = new JTextField(28);
        txtPacApellidos = new JTextField(28);
        cbPacGenero = new JComboBox<>(new String[]{"Masculino", "Femenino", "Otro", "No especificado"});
        txtPacFechaNac = new JTextField("1995-01-01", 15);
        txtPacNss = new JTextField(15);
        txtPacTelCel = new JTextField(18);
        txtPacContactoNom = new JTextField(28);
        txtPacContactoTel = new JTextField(18);
        txtDirCalle = new JTextField(32);
        txtDirCiudad = new JTextField("Hermosillo, Sonora", 25);
        txtDirCp = new JTextField(10);

        int r = 0;
        addRow(form, gbc, r++, "Nombre *:", txtPacNombre, 1);
        addRow(form, gbc, r++, "Apellidos:", txtPacApellidos, 1);
        addRow(form, gbc, r++, "Género *:", cbPacGenero, 1);
        addRow(form, gbc, r++, "Fecha nac. (YYYY-MM-DD) *:", txtPacFechaNac, 1);
        addRow(form, gbc, r++, "NSS (11 dígitos):", txtPacNss, 1);
        addRow(form, gbc, r++, "Tel. celular:", txtPacTelCel, 1);
        addRow(form, gbc, r++, "Contacto emergencia:", txtPacContactoNom, 1);
        addRow(form, gbc, r++, "Tel. emergencia:", txtPacContactoTel, 1);
        addRow(form, gbc, r++, "Calle, núm. y colonia:", txtDirCalle, 1);
        addRow(form, gbc, r++, "Ciudad y estado:", txtDirCiudad, 1);
        addRow(form, gbc, r++, "Código postal (5 dígitos):", txtDirCp, 1);

        JButton btnGuardarPac = new JButton("Guardar Paciente");
        btnGuardarPac.addActionListener(e -> guardarPaciente());
        gbc.gridy = r;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        form.add(btnGuardarPac, gbc);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private void guardarPaciente() {
        String nombre = txtPacNombre.getText().trim();
        String apellidos = txtPacApellidos.getText().trim();
        String genero = (String) cbPacGenero.getSelectedItem();
        String fnacStr = txtPacFechaNac.getText().trim();
        String nss = txtPacNss.getText().trim();
        String telCel = txtPacTelCel.getText().trim();
        String cNom = txtPacContactoNom.getText().trim();
        String cTel = txtPacContactoTel.getText().trim();
        String calle = txtDirCalle.getText().trim();
        String ciudad = txtDirCiudad.getText().trim();
        String cp = txtDirCp.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del paciente es obligatorio.", "Dato faltante", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fnac;
        try {
            fnac = LocalDate.parse(fnacStr);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Usa el formato YYYY-MM-DD para la fecha de nacimiento.", "Fecha inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!nss.isEmpty() && !nss.matches("[0-9]{11}")) {
            JOptionPane.showMessageDialog(this, "El NSS debe contener exactamente 11 dígitos.", "NSS inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!calle.isEmpty() && !cp.matches("[0-9]{5}")) {
            JOptionPane.showMessageDialog(this, "Si registras dirección, el código postal debe tener 5 dígitos.", "CP inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String insPaciente =
                "INSERT INTO pacientes (" +
                "  nombre, apellidos, genero, fecha_nacimiento, nss," +
                "  tel_celular, nombre_contacto, telefono_contacto" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING paciente_id";

        String insDireccion =
                "INSERT INTO direcciones (" +
                "  empleado_id, paciente_id, numero_calle_colonia," +
                "  ciudad_estado, codigo_postal, fecha_inicio, fecha_fin" +
                ") VALUES (NULL, ?, ?, ?, ?, CURRENT_DATE, NULL)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int nuevoPid;
                try (PreparedStatement ps = conn.prepareStatement(insPaciente)) {
                    ps.setString(1, nombre);
                    setNullableString(ps, 2, apellidos);
                    ps.setString(3, genero);
                    ps.setDate(4, Date.valueOf(fnac));
                    setNullableString(ps, 5, nss);
                    setNullableString(ps, 6, telCel);
                    setNullableString(ps, 7, cNom);
                    setNullableString(ps, 8, cTel);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        nuevoPid = rs.getInt(1);
                    }
                }

                if (!calle.isEmpty()) {
                    try (PreparedStatement psDir = conn.prepareStatement(insDireccion)) {
                        psDir.setInt(1, nuevoPid);
                        psDir.setString(2, calle);
                        psDir.setString(3, ciudad.isEmpty() ? "Sin especificar" : ciudad);
                        psDir.setString(4, cp);
                        psDir.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Paciente registrado con ID #" + nuevoPid + ".", "Paciente guardado", JOptionPane.INFORMATION_MESSAGE);
                txtPacNombre.setText("");
                txtPacApellidos.setText("");
                txtPacNss.setText("");
                txtPacTelCel.setText("");
                txtPacContactoNom.setText("");
                txtPacContactoTel.setText("");
                txtDirCalle.setText("");
                txtDirCp.setText("");
                cargarCatalogos();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el paciente:\n" + ex.getMessage(),
                    "Error en PostgreSQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setNullableString(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.isEmpty()) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, val);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new hospital_db().setVisible(true);
        });
    }
}