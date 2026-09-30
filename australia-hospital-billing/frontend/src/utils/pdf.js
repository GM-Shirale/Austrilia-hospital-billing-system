import { label } from './format';

/*
 * Tax Invoice / Discharge Summary (A4), generated in the browser from GET /bills/{id}/invoice.
 * jsPDF + autotable are loaded on demand so they are not part of the initial bundle.
 * Only ASCII text is used: the built-in PDF fonts have no glyphs for fancy symbols.
 */

const NAVY = [15, 36, 84];
const BLUE = [37, 99, 235];
const SLATE = [71, 85, 105];
const LIGHT = [241, 245, 249];
const GREEN = [5, 150, 105];
const AMBER = [217, 119, 6];
const RED = [225, 29, 72];

const aud = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });
const money = (v) => (v === null || v === undefined ? '-' : aud.format(Number(v)));
const d2 = (n) => String(n).padStart(2, '0');
const fmtDate = (v) => {
  if (!v) return '-';
  const d = new Date(v);
  return `${d2(d.getDate())}/${d2(d.getMonth() + 1)}/${d.getFullYear()}`;
};
const fmtDateTime = (v) => {
  if (!v) return '-';
  const d = new Date(v);
  return `${fmtDate(v)} ${d2(d.getHours())}:${d2(d.getMinutes())}`;
};
/** 2123456071 -> "2123 45607 1" (layout printed on the green Medicare card). */
const medicareCard = (no) => (no && no.length === 10 ? `${no.slice(0, 4)} ${no.slice(4, 9)} ${no.slice(9)}` : no || 'Not provided');
const abn = (v) => (v && v.length === 11 ? `${v.slice(0, 2)} ${v.slice(2, 5)} ${v.slice(5, 8)} ${v.slice(8)}` : v || '-');
const phone = (v) => (v && v.length === 10 ? `(${v.slice(0, 2)}) ${v.slice(2, 6)} ${v.slice(6)}` : v || '');

const CATEGORIES = [
  { key: 'ROOM', title: 'Accommodation & nursing', types: ['ROOM', 'NURSING'] },
  { key: 'CONSULT', title: 'Doctor consultations & procedures', types: ['DOCTOR', 'CONSULTATION', 'SURGERY', 'PROCEDURE'] },
  { key: 'LAB', title: 'Pathology & diagnostic tests', types: ['LAB'] },
  { key: 'PHARMACY', title: 'Pharmacy', types: ['PHARMACY'] },
  { key: 'OTHER', title: 'Equipment & other', types: ['EQUIPMENT', 'OTHER'] },
];

const sum = (items, field) => items.reduce((acc, i) => acc + Number(i[field] || 0), 0);

/** Groups bill lines into invoice sections with subtotals. */
export function invoiceSections(items) {
  return CATEGORIES.map((c) => {
    const lines = items.filter((i) => c.types.includes(i.itemType));
    return {
      ...c,
      lines,
      net: sum(lines, 'netAmount'),
      gst: sum(lines, 'gstAmount'),
      medicare: sum(lines, 'medicareBenefit'),
      fund: sum(lines, 'insuranceCoveredAmount'),
      patient: sum(lines, 'patientAmount'),
    };
  }).filter((s) => s.lines.length);
}

export async function downloadTaxInvoicePdf(invoice) {
  const [{ jsPDF }, { default: autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  const { hospital, patient, attendingDoctor: doctor, stay, coverage, bill, payments } = invoice;
  const doc = new jsPDF({ unit: 'mm', format: 'a4' });
  const W = doc.internal.pageSize.getWidth();
  const M = 14;

  // ------------------------------------------------------------------ letterhead
  doc.setFillColor(...NAVY);
  doc.rect(0, 0, W, 34, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(17);
  doc.text(hospital.name, M, 13);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  const street = [hospital.addressLine, [hospital.suburb, hospital.state, hospital.postcode].filter(Boolean).join(' ')]
    .filter(Boolean).join(', ');
  doc.text(street || hospital.state, M, 19);
  doc.text(`ABN ${abn(hospital.abn)}   ${phone(hospital.phone)}   ${hospital.email || ''}`.trim(), M, 24);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(15);
  doc.text('TAX INVOICE', W - M, 13, { align: 'right' });
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.text('& Discharge Summary', W - M, 19, { align: 'right' });
  doc.text(`Invoice no: ${invoice.invoiceNo}`, W - M, 24, { align: 'right' });
  doc.text(`Issued: ${fmtDateTime(invoice.issuedAt)}`, W - M, 29, { align: 'right' });

  // payment status pill
  const statusColour = { FULL: GREEN, PARTIAL: AMBER, PENDING: RED }[bill.paymentStatus] || SLATE;
  const statusText = { FULL: 'PAID IN FULL', PARTIAL: 'PARTIALLY PAID', PENDING: 'PAYMENT PENDING' }[bill.paymentStatus] || bill.paymentStatus;
  doc.setFillColor(...statusColour);
  doc.roundedRect(M, 38, 42, 7, 1.5, 1.5, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(8);
  doc.text(statusText, M + 21, 42.6, { align: 'center' });
  doc.setTextColor(...SLATE);
  doc.setFont('helvetica', 'normal');
  doc.text(`Bill status: ${label(bill.status)}${bill.chargesLockedAt ? `   |   Charges locked at discharge ${fmtDateTime(bill.chargesLockedAt)}` : ''}`,
    M + 46, 42.6);

  // ------------------------------------------------------------------ patient + stay boxes
  const boxTop = 49;
  const boxW = (W - 2 * M - 6) / 2;
  const drawBox = (x, title, rows) => {
    doc.setFillColor(...LIGHT);
    doc.roundedRect(x, boxTop, boxW, 55, 2, 2, 'F');
    doc.setTextColor(...BLUE);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.text(title, x + 4, boxTop + 6);
    doc.setFontSize(8);
    rows.forEach(([k, v], i) => {
      const y = boxTop + 12 + i * 5.2;
      doc.setTextColor(...SLATE);
      doc.setFont('helvetica', 'normal');
      doc.text(k, x + 4, y);
      doc.setTextColor(15, 23, 42);
      doc.setFont('helvetica', 'bold');
      const value = doc.splitTextToSize(String(v ?? '-'), boxW - 36)[0];
      doc.text(value, x + 32, y);
    });
  };
  drawBox(M, 'PATIENT', [
    ['Name', patient.name],
    ['MRN', patient.mrn],
    ['Date of birth', `${fmtDate(patient.dob)}${patient.gender ? `  (${label(patient.gender)})` : ''}`],
    ['Medicare card', medicareCard(patient.medicareNo)],
    ['Medicare IRN', patient.medicareIrn ?? '-'],
    ['Address', patient.address || '-'],
    ['Phone', patient.phone || '-'],
    ['Email', patient.email || '-'],
  ]);
  drawBox(M + boxW + 6, 'ADMISSION', [
    ['Admission no', `${stay.admissionNo}  (${stay.careSetting === 'OPD' ? 'Out-patient' : 'In-patient'})`],
    ['Type / class', `${stay.admissionType} / ${label(stay.financialClass)}`],
    ['Admitted', fmtDateTime(stay.admissionDate)],
    ['Discharged', stay.dischargeDate ? fmtDateTime(stay.dischargeDate) : 'Still admitted'],
    ['Length of stay', `${stay.lengthOfStayDays} day(s)`],
    ['Attending doctor', doctor.name],
    ['Specialty', `${doctor.specialization}, ${doctor.department}`],
    ['Provider no', doctor.providerNo],
  ]);

  let y = boxTop + 61;
  doc.setFontSize(8);
  doc.setTextColor(...SLATE);
  doc.setFont('helvetica', 'normal');
  const cover = coverage?.fundName
    ? `Health fund: ${coverage.fundName} ${label(coverage.coverTier)} cover, policy ${coverage.policyNo} (${label(coverage.networkStatus)} agreement)`
    : 'Health fund: none on file (Medicare / self-funded)';
  doc.text(cover, M, y);
  y += 4.5;
  const diagnosis = doc.splitTextToSize(`Diagnosis / reason for admission: ${stay.diagnosis || '-'}`, W - 2 * M);
  doc.text(diagnosis, M, y);
  y += diagnosis.length * 4 + 1;
  if (stay.beds?.length) {
    const beds = stay.beds.map((b) => `${b.room} ${label(b.roomType)} ${fmtDate(b.fromDate)}-${b.toDate ? fmtDate(b.toDate) : 'now'} @ ${money(b.ratePerDay)}/day`);
    const bedLines = doc.splitTextToSize(`Bed(s): ${beds.join('; ')}`, W - 2 * M);
    doc.text(bedLines, M, y);
    y += bedLines.length * 4 + 1;
  }

  // ------------------------------------------------------------------ itemised charges
  const sections = invoiceSections(bill.items);
  const body = [];
  sections.forEach((s) => {
    body.push([{ content: s.title.toUpperCase(), colSpan: 8, styles: { fillColor: [226, 232, 240], fontStyle: 'bold', textColor: NAVY } }]);
    s.lines.forEach((i) => body.push([
      i.description,
      i.mbsItemNo || '',
      Number(i.quantity).toString(),
      money(i.unitPrice),
      money(i.netAmount),
      money(i.medicareBenefit),
      money(i.insuranceCoveredAmount),
      money(i.patientAmount),
    ]));
    body.push([
      { content: `Subtotal - ${s.title}`, colSpan: 4, styles: { halign: 'right', fontStyle: 'bold' } },
      { content: money(s.net), styles: { fontStyle: 'bold' } },
      { content: money(s.medicare), styles: { fontStyle: 'bold' } },
      { content: money(s.fund), styles: { fontStyle: 'bold' } },
      { content: money(s.patient), styles: { fontStyle: 'bold' } },
    ]);
  });
  autoTable(doc, {
    startY: y + 1,
    margin: { left: M, right: M, bottom: 24 },
    head: [['Description', 'MBS', 'Qty', 'Rate', 'Charge', 'Medicare', 'Fund', 'Patient']],
    body,
    theme: 'grid',
    styles: { fontSize: 7.2, cellPadding: 1.6, lineColor: [226, 232, 240], lineWidth: 0.1, textColor: [15, 23, 42] },
    headStyles: { fillColor: NAVY, textColor: 255, fontStyle: 'bold' },
    columnStyles: {
      0: { cellWidth: 70 }, 1: { halign: 'center', cellWidth: 12 }, 2: { halign: 'right', cellWidth: 10 },
      3: { halign: 'right' }, 4: { halign: 'right' }, 5: { halign: 'right' }, 6: { halign: 'right' }, 7: { halign: 'right' },
    },
  });
  y = doc.lastAutoTable.finalY + 6;

  // ------------------------------------------------------------------ summary + totals
  if (y > 225) {
    doc.addPage();
    y = 20;
  }
  const byKey = Object.fromEntries(sections.map((s) => [s.key, s.net]));
  autoTable(doc, {
    startY: y,
    margin: { left: M, right: W / 2 + 3 },
    head: [['Charge summary', 'Amount']],
    body: [
      ['Room / accommodation', money(byKey.ROOM || 0)],
      ['Consultations & procedures', money(byKey.CONSULT || 0)],
      ['Lab & diagnostic tests', money(byKey.LAB || 0)],
      ['Pharmacy', money(byKey.PHARMACY || 0)],
      ['Other', money(byKey.OTHER || 0)],
      [{ content: 'GST included', styles: { textColor: SLATE } }, { content: money(bill.gstAmount), styles: { textColor: SLATE } }],
    ],
    theme: 'striped',
    styles: { fontSize: 8, cellPadding: 1.8 },
    headStyles: { fillColor: BLUE },
    columnStyles: { 1: { halign: 'right' } },
  });
  const totalsRows = [
    ['Total charges', money(bill.netAmount)],
    [`Less Medicare rebate (${stay.financialClass === 'PUBLIC' ? '100% public' : stay.careSetting === 'OPD' ? '85% OPD' : '75% IPD'})`, `-${money(bill.medicareAmount)}`],
    ['Less health fund benefit', `-${money(bill.insuranceAmount)}`],
    ['TOTAL DUE (patient)', money(bill.patientPayableAmount)],
    ['Amount paid', money(bill.patientPaidAmount)],
    ['BALANCE REMAINING', money(bill.patientBalance)],
  ];
  autoTable(doc, {
    startY: y,
    margin: { left: W / 2 + 3, right: M },
    head: [['Account', 'AUD']],
    body: totalsRows,
    theme: 'plain',
    styles: { fontSize: 8.5, cellPadding: 1.8 },
    headStyles: { fillColor: NAVY, textColor: 255 },
    columnStyles: { 1: { halign: 'right' } },
    didParseCell: (data) => {
      if (data.section === 'body' && (data.row.index === 3 || data.row.index === 5)) {
        data.cell.styles.fontStyle = 'bold';
        data.cell.styles.fillColor = data.row.index === 5 ? [254, 243, 199] : LIGHT;
        if (data.row.index === 5) data.cell.styles.fontSize = 10;
      }
    },
  });
  y = Math.max(doc.lastAutoTable.finalY, y) + 6;

  // ------------------------------------------------------------------ payments
  if (payments?.length) {
    if (y > 250) {
      doc.addPage();
      y = 20;
    }
    autoTable(doc, {
      startY: y,
      margin: { left: M, right: M, bottom: 24 },
      head: [['Payment no', 'Date', 'Paid by', 'Method', 'Reference', 'Amount']],
      body: payments.map((p) => [p.paymentNo, fmtDateTime(p.paymentDate), label(p.paidBy), label(p.paymentMode), p.transactionNo || '-', money(p.amount)]),
      theme: 'striped',
      styles: { fontSize: 7.5, cellPadding: 1.6 },
      headStyles: { fillColor: SLATE },
      columnStyles: { 5: { halign: 'right' } },
    });
    y = doc.lastAutoTable.finalY + 6;
  }

  // ------------------------------------------------------------------ footer on every page
  const pages = doc.getNumberOfPages();
  for (let p = 1; p <= pages; p += 1) {
    doc.setPage(p);
    const H = doc.internal.pageSize.getHeight();
    doc.setDrawColor(226, 232, 240);
    doc.line(M, H - 20, W - M, H - 20);
    doc.setFontSize(6.8);
    doc.setTextColor(...SLATE);
    doc.setFont('helvetica', 'normal');
    const note = doc.splitTextToSize(`${invoice.gstStatement} Medicare rebates: 75% of the MBS schedule fee for in-patient `
      + 'services, 85% for out-patient services, public patients fully funded; no Medicare rebate on accommodation or pharmacy.', W - 2 * M);
    doc.text(note, M, H - 16);
    doc.text(`Generated by AustraCare MBS Pro - ${hospital.name}`, M, H - 5);
    doc.text(`Page ${p} of ${pages}`, W - M, H - 5, { align: 'right' });
  }

  doc.save(`TaxInvoice_${invoice.invoiceNo}.pdf`);
}
