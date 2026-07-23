import EmployeeFormSection from "../features/admin/employees/components/EmployeeFormSection";
import EmployeeListSection from "../features/admin/employees/components/EmployeeListSection";
import EmployeeManagementHeader from "../features/admin/employees/components/EmployeeManagementHeader";
import useEmployeeManagement from "../features/admin/employees/hooks/useEmployeeManagement";

function AdminEmployeesPage() {
  const {
    loading,
    saving,
    statusChangingId,
    errorMessage,
    successMessage,
    selectedRole,
    selectedEmployeeId,
    showEmployeeForm,
    editingEmployeeId,
    fullName,
    email,
    password,
    role,
    employeesForSelectedRole,
    filteredEmployees,
    resetForm,
    loadEmployees,
    handleOpenCreateForm,
    handleOpenEditForm,
    handleEmployeeSubmit,
    handleChangeStatus,
    handleFullNameChange,
    handleEmailChange,
    handlePasswordChange,
    handleRoleChange,
    handleSelectedRoleChange,
    handleSelectedEmployeeChange,
  } = useEmployeeManagement();

  return (
    <div className="admin-page">
      <EmployeeManagementHeader
        loading={loading}
        onReload={loadEmployees}
        onOpenCreateForm={handleOpenCreateForm}
      />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {successMessage && <p className="feedback-message">{successMessage}</p>}

      {showEmployeeForm && (
        <EmployeeFormSection
          editingEmployeeId={editingEmployeeId}
          fullName={fullName}
          email={email}
          password={password}
          role={role}
          saving={saving}
          onFullNameChange={handleFullNameChange}
          onEmailChange={handleEmailChange}
          onPasswordChange={handlePasswordChange}
          onRoleChange={handleRoleChange}
          onSubmit={handleEmployeeSubmit}
          onCancel={resetForm}
        />
      )}

      <EmployeeListSection
        loading={loading}
        selectedRole={selectedRole}
        selectedEmployeeId={selectedEmployeeId}
        employeesForSelectedRole={employeesForSelectedRole}
        filteredEmployees={filteredEmployees}
        statusChangingId={statusChangingId}
        onRoleChange={handleSelectedRoleChange}
        onEmployeeChange={handleSelectedEmployeeChange}
        onEdit={handleOpenEditForm}
        onChangeStatus={handleChangeStatus}
      />
    </div>
  );
}

export default AdminEmployeesPage;
