import { useEffect, useState } from 'react';
import { admissionApi } from '../api';

/** Dropdown of currently ADMITTED patients (used by lab orders and prescriptions). */
export default function AdmissionSelect({ value, onChange }) {
  const [admissions, setAdmissions] = useState([]);
  useEffect(() => {
    admissionApi.search('ADMITTED', 0, 100).then((page) => {
      setAdmissions(page.content);
      if (!value && page.content[0]) onChange(page.content[0]);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <select
      className="input"
      value={value?.id ?? ''}
      onChange={(e) => onChange(admissions.find((a) => String(a.id) === e.target.value))}
    >
      {admissions.length === 0 && <option value="">No admitted patients</option>}
      {admissions.map((a) => (
        <option key={a.id} value={a.id}>
          {a.patientName} - {a.admissionNo}
        </option>
      ))}
    </select>
  );
}
