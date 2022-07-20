{
  title: 'SMTP Connector',
  secure_tunnel: true,

  connection: {
    fields: [{ name: 'profile', hint: 'SMTP Connector' }],
    authorization: { type: 'none'}
  },

  test: lambda do |connection|
    true
  end,

  actions: {

    sendEmail: {
      title: 'Send Email',
      description: 'Send Email via SMTP',
      input_fields:lambda do |object_definitions|
        object_definitions['email']
      end,
      output_fields:lambda do |object_definitions|
        object_definitions['status']
      end,

      execute: lambda do |connection|
        get("http://localhost/ext/#{connection['profile']}/sendEmail").headers('X-Workato-Connector': 'enforce')
      end
    },
    
  },

  pick_lists: {
    emailMimeType: lambda do |connection|
      [
        [ "Plain Text", "text/plain; charset=UTF-8" ],
        [ "HTML Formated", "text/html; charset=UTF-8" ]
      ]
    end
  },

  object_definitions:{

    status:{
      fields: lambda do
        [
          {
            "control_type": "text",
            "label": "Status",
            "type": "string",
            "name": "status",
            "optional": false
          }
        ]
      end
    },

    email:{
      fields: lambda do
        [
          {
            "control_type": "text",
            "label": "From",
            "type": "string",
            "name": "from",
            "optional": false
          },
          {
            "control_type": "text",
            "label": "Recipient(s)",
            "type": "string",
            "name": "recipients",
            "optional": false
          },
          {
            "control_type": "text",
            "label": "Subject",
            "type": "string",
            "name": "subject",
            "optional": false
          },
          # {
          #   "control_type": "select",
          #   "label": "Email Mime Type",
          #   pick_list: 'emailMimeType',
          #   "name": "emailMimeType",
          #   "optional": false
          # },
          {
            "control_type": "text-area",
            "label": "Email Body",
            "type": "string",
            "name": "emailBody",
            "optional": false
          }          
        ]
      end
    }
  }
}