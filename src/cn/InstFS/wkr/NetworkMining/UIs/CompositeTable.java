package cn.InstFS.wkr.NetworkMining.UIs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.InstFS.wkr.NetworkMining.TaskConfigure.association.AssociationMingObject;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
//import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import associationRules.ProtoclPair;

public class CompositeTable extends Composite {

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 *            、
	 * 
	 * @param style
	 */
	Label iplabel = null;
	Table table = null;
	CTabFolderChart tab = null;
	TableItem[] itemList = null;
	int[] index = null;
	CompositeMainProtocolConfidence compositeMainProtocolConfidence=null;
	Map<Integer, CTabItem> maptabitem;
	String mingObj;

	public CompositeTable(Composite parent, int style, String ip,
			List<ProtoclPair> protocolPairList, CTabFolderChart tab, CompositeMainProtocolConfidence compositeMainProtocolConfidence,String mingObj) {
		super(parent, style);
		this.tab = tab;
		this.compositeMainProtocolConfidence=compositeMainProtocolConfidence;
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		this.setLayout(layout);

		iplabel = new Label(this, SWT.NULL);
		iplabel.setText(ip);
		maptabitem = new HashMap<Integer, CTabItem>();
		this.mingObj = mingObj;
		createTable(this, SWT.NULL, protocolPairList);

	}

	private void createTable(Composite composite, int style,
			final List<ProtoclPair> protocolPairList) {

		GridData griddata = new org.eclipse.swt.layout.GridData();
		griddata.horizontalAlignment = SWT.FILL;
		griddata.grabExcessHorizontalSpace = true;
		griddata.grabExcessVerticalSpace = true;
		griddata.verticalAlignment = SWT.FILL;

		// �������
		table = new Table(composite, SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLayoutData(griddata);
		table.setLinesVisible(true);
		// 位每个表格项设置监听 ，更新tab打开标志
		table.addListener(SWT.MouseDoubleClick, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				// TODO Auto-generated method stub
				// TableItem[] itemList=table.getItems();
				int itemIndex = table.getSelectionIndex();
				System.out.println("选中 itemIndex:"+itemIndex);
				if(itemIndex>-1){
								if (compositeMainProtocolConfidence.tableIndex[itemIndex] == 0) {
					// 问题出在这
					Iterator it = protocolPairList.iterator();
					TableItem tableItem = table.getItem(itemIndex);
					while (it.hasNext()) {
						ProtoclPair pp = (ProtoclPair) it.next();
						if ((pp.getProtocol1().equals(tableItem.getText(1)))
								&& (pp.getProtocol2().equals(tableItem
										.getText(2)))) {
							// System.out.println("table协议匹配ok");
							CTabItem tabitem = tab.createTabItem(
									itemList[itemIndex].getText(1) + "/"
											+ itemList[itemIndex].getText(2),
									itemIndex, pp);
							maptabitem.put(itemIndex, tabitem);
							compositeMainProtocolConfidence.tableIndex[itemIndex] = 1;

						}
					}

				} else {
					// 选中对应的tab
					tab.setSelection(maptabitem.get(itemIndex));
					// tab.setSelection(tab.getItem(itemIndex));

				}
				}
	

			}

		});
		// ������ͷ�ַ�
		final String[] tableheader;
		if (mingObj.equals(AssociationMingObject.IPInnerPortMing.toString())) {
			tableheader = new String[]{"序号", "协议名", "协议名", "置信度", "兴趣度"};
		} else {
			tableheader = new String[] {"序号", "IP地址", "IP地址", "置信度", "兴趣度"};
		}
		// TableSorter tablesorter=new TableSorter();
		for (int i = 0; i < tableheader.length; i++) {
			final TableColumn c1 = new TableColumn(table, SWT.None);
			c1.setText(tableheader[i]);

		}

		// 添加数据

		Iterator it1 = protocolPairList.iterator();
		int countIndex = 0;
		while (it1.hasNext()) {
			ProtoclPair temp = (ProtoclPair) it1.next();
			/*
			 * System.out.println("协议1：" + temp.getProtocol1() + "  协议2：" +
			 * temp.getProtocol1() + "	置信度：" + temp.confidence);
			 */
			final TableItem t = new TableItem(table, SWT.None);
			t.setText(new String[] { "" + (countIndex++),
					"" + temp.getProtocol1(), "" + temp.getProtocol2(),
					"" + temp.confidence,""+temp.getInf() });
			// t.addListener(SWT.Selection, listener);
		}

		itemList = table.getItems();
		// 重新刷新表格
		for (int i = 0; i < tableheader.length; i++) {
			table.getColumn(i).pack();
		}

	}

	public static void main(String[] args) {
		// TableCoposite table=new TableComposite();

	}

	public int getTableIndexCount() {
		// TODO Auto-generated method stub
		return table.getItemCount();
	}

}
