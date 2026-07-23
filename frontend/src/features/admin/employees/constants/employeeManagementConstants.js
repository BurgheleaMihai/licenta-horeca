export const roleLabels = {
  WAITER: "Ospatar",
  KITCHEN: "Bucatarie",
  BAR: "Bar",
  MANAGER: "Manager",
  ADMIN: "Administrator",
};

export const employeeActionStyles = {
  container: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "10px",
    flexWrap: "wrap",
    minWidth: "230px",
  },
  editButton: {
    minWidth: "96px",
    padding: "9px 14px",
    border: "1px solid #cbd5e1",
    borderRadius: "8px",
    backgroundColor: "#f8fafc",
    color: "#1e293b",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  disableButton: {
    minWidth: "112px",
    padding: "9px 14px",
    border: "1px solid #fecaca",
    borderRadius: "8px",
    backgroundColor: "#fee2e2",
    color: "#b91c1c",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  enableButton: {
    minWidth: "112px",
    padding: "9px 14px",
    border: "1px solid #bbf7d0",
    borderRadius: "8px",
    backgroundColor: "#dcfce7",
    color: "#15803d",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  disabledButton: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};

export const employeeFormActionStyles = {
  container: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "12px",
    flexWrap: "wrap",
    marginTop: "18px",
  },
  button: {
    width: "200px",
    minHeight: "46px",
    padding: "10px 16px",
    borderRadius: "9px",
    fontSize: "15px",
    fontWeight: "700",
    cursor: "pointer",
  },
  saveButton: {
    border: "1px solid #222222",
    backgroundColor: "#222222",
    color: "#ffffff",
  },
  cancelButton: {
    border: "1px solid #cbd5e1",
    backgroundColor: "#f8fafc",
    color: "#1e293b",
  },
  disabledButton: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};
