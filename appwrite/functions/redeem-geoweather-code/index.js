import { Client, TablesDB, Query, ID } from "node-appwrite";

export default async ({ req, res, error }) => {
  try {
    const userId = req.headers["x-appwrite-user-id"];
    if (!userId) return res.json({ ok: false, error: "Authentication required" }, 401);

    const body = req.bodyJson ?? {};
    const code = String(body.code ?? "").trim();
    if (!code) return res.json({ ok: false, error: "Code required" }, 400);

    const client = new Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(req.headers["x-appwrite-key"]);
    const db = new TablesDB(client);

    const codes = await db.listRows({
      databaseId: "geoweather",
      tableId: "geoweather_codes",
      queries: [Query.equal("code", code), Query.equal("is_used", false), Query.limit(1)]
    });
    const row = codes.rows[0];
    if (!row) return res.json({ ok: false, error: "invalid_code" }, 404);

    const now = new Date().toISOString();
    await db.updateRow({
      databaseId: "geoweather",
      tableId: "geoweather_codes",
      rowId: row.$id,
      data: { is_used: true, used_by: userId, used_at: now }
    });

    const existing = await db.listRows({
      databaseId: "geoweather",
      tableId: "geoweather_subscriptions",
      queries: [Query.equal("user_id", userId), Query.equal("is_active", true), Query.limit(25)]
    });
    for (const subscription of existing.rows) {
      await db.updateRow({
        databaseId: "geoweather",
        tableId: "geoweather_subscriptions",
        rowId: subscription.$id,
        data: { is_active: false, updated_at: now }
      });
    }

    await db.createRow({
      databaseId: "geoweather",
      tableId: "geoweather_subscriptions",
      rowId: ID.unique(),
      data: {
        user_id: userId,
        location: "GeoWeather Android",
        type: row.type,
        is_active: true,
        created_at: now,
        updated_at: now,
        redeemed_code_id: row.$id
      }
    });

    return res.json({ ok: true, subscription: row.type });
  } catch (e) {
    error(e.message);
    return res.json({ ok: false, error: "redeem_failed" }, 500);
  }
};
